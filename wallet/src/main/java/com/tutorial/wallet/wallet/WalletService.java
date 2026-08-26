package com.tutorial.wallet.wallet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorial.sharedmodule.infra.outbox.OutBox;
import com.tutorial.shared.wallet.events.*;
import com.tutorial.sharedmodule.infra.outbox.OutBoxRepository;
import com.tutorial.wallet.wallet.dto.*;
import com.tutorial.wallet.currency.CurrencyApiImpl;
import jakarta.persistence.EntityNotFoundException;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

  private final WalletRepository walletRepository;
  private final CurrencyApiImpl currencyApi;
  private final TransactionTemplate transactionTemplate;
  private final OutBoxRepository outBoxRepository;
  private final ObjectMapper objectMapper;

  @Value("app.wallet.outbox.aggregate-type")
  private String aggregateType;

  @Value("app.wallet.outbox.withdraw.success-topic")
  private String successTopic;

  @Value("app.wallet.outbox.withdraw.failed-topic")
  private String failedTopic;

  @Autowired
  public WalletService(
      WalletRepository walletRepository,
      CurrencyApiImpl currencyApi,
      TransactionTemplate transactionTemplate,
      OutBoxRepository outBoxRepository,
      ObjectMapper objectMapper) {
    this.walletRepository = walletRepository;
    this.currencyApi = currencyApi;
    this.transactionTemplate = transactionTemplate;
    this.outBoxRepository = outBoxRepository;
    this.objectMapper = objectMapper;
  }

  private record WithdrawalWallets(Wallet userWallet, Wallet systemWallet) {}

  public WalletResponseDto createWallet(WalletRequestDto walletRequestDto, Long userId) {
    if (currencyApi.currencyExistsById(walletRequestDto.currencyId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Currency Not Found");
    }
    if (walletRepository.existsByUserIdAndCurrencyId(userId, walletRequestDto.currencyId())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Wallet With Currency Already exists");
    }
    Wallet wallet =
        new Wallet(walletRequestDto.name(), BigDecimal.ZERO, userId, walletRequestDto.currencyId());
    walletRepository.save(wallet);
    return returnWalletResponse(wallet);
  }

  public WalletResponseDto getWalletWithId(Long id) {
    Wallet wallet = getWalletById(id);
    return returnWalletResponse(wallet);
  }

  public WalletResponseDto updateWallet(Long id, WalletRequestDto walletRequestDto) {
    Wallet wallet = getWalletById(id);
    wallet.setName(walletRequestDto.name());
    Wallet updatedWallet = walletRepository.save(wallet);
    return returnWalletResponse(updatedWallet);
  }

  private Wallet getWalletById(Long id) {
    return walletRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet Not Found"));
  }

  private WalletResponseDto returnWalletResponse(Wallet wallet) {
    return new WalletResponseDto(
        wallet.getName(),
        wallet.getId(),
        wallet.getBalance(),
        wallet.getCurrencyId(),
        wallet.getCreatedAt());
  }

  public @Nullable List<WalletResponseDto> getUserWallets(Long userId) {
    return walletRepository.findByUserId(userId).stream().map(this::returnWalletResponse).toList();
  }

  public void createDefaultWallet(Long userId) {
    walletRepository.save(
        new Wallet(String.format("USDT-%d", userId), BigDecimal.ZERO, userId, 1L));
  }

  private boolean isWalletBalanceGoesToNegative(Wallet wallet, BigDecimal amount) {
    return wallet.getBalance().subtract(amount).compareTo(BigDecimal.ZERO) < 0;
  }

  private boolean isWalletFrozen(Wallet wallet) {
    return wallet.getWalletStatus() == WalletStatus.FROZEN;
  }

  private WithdrawalWallets resolveAndValidate(Long currencyId, Long userId) {
    Wallet userWallet =
        walletRepository
            .findByCurrencyIdAndUserId(currencyId, userId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Wallet With " + currencyId + "for user not found"));
    Wallet systemWallet =
        walletRepository
            .findByCurrencyIdAndUserId(currencyId, 10203048859L)
            .orElseThrow(
                () -> new EntityNotFoundException("System Wallet With This Currency Not Found"));
    if (isWalletFrozen(userWallet)) {
      throw new ForbiddenActionOnFreezeWalletException(
          "Wallet Is Freeze And You Cant Do Any Action WithIt");
    }
    if (isWalletFrozen(systemWallet)) {
      throw new ForbiddenActionOnFreezeWalletException(
          "The Withdraw On This Currency " + currencyId + "Is Freeze For Now");
    }
    return new WithdrawalWallets(userWallet, systemWallet);
  }

  public void withdraw(WithdrawDtoEvent withdrawDtoEvent) {
    final WithdrawalWallets withdrawalWallets;
    try {
      withdrawalWallets =
          resolveAndValidate(withdrawDtoEvent.getCurrencyId(), withdrawDtoEvent.getUserId());
    } catch (ForbiddenActionOnFreezeWalletException e) {
      WithdrawFailedDtoEvent withdrawFailedDtoEvent =
          buildFailureEvent(withdrawDtoEvent, e.getMessage(), 0);
      saveFailedOutboxEvent(
          "withdraw", withdrawDtoEvent.getIdempotencyKey(), withdrawFailedDtoEvent);
      throw e;
    } catch (EntityNotFoundException e) {
      WithdrawFailedDtoEvent withdrawFailedDtoEvent =
          buildFailureEvent(withdrawDtoEvent, e.getMessage(), 1);
      saveFailedOutboxEvent(
          "withdraw", withdrawDtoEvent.getIdempotencyKey(), withdrawFailedDtoEvent);
      throw e;
    }
    BigDecimal bigDecimalAmount = new BigDecimal(withdrawDtoEvent.getAmount());
    try {
      transactionTemplate.execute(
          status -> {
            Wallet userWallet =
                walletRepository
                    .findByIdForUpdate(withdrawalWallets.userWallet.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));
            if (isWalletBalanceGoesToNegative(userWallet, bigDecimalAmount)) {
              throw new InsufficientWalletBalanceException("Insufficient Wallet Balance");
            }
            Wallet systemWallet =
                walletRepository
                    .findByIdForUpdate(withdrawalWallets.systemWallet.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));
            userWallet.setBalance(userWallet.getBalance().subtract(bigDecimalAmount));
            systemWallet.setBalance(systemWallet.getBalance().add(bigDecimalAmount));
            WithdrawSuccessDtoEvent withdrawSuccessDtoEvent = buildSuccessEvent(withdrawDtoEvent);
            saveSuccessOutboxEvent(
                "withdraw", withdrawSuccessDtoEvent.getIdempotencyKey(), withdrawSuccessDtoEvent);
            return null;
          });
    } catch (EntityNotFoundException e) {
      WithdrawFailedDtoEvent withdrawFailedDtoEvent =
          buildFailureEvent(withdrawDtoEvent, e.getMessage(), 3);
      saveFailedOutboxEvent(
          "withdraw", withdrawFailedDtoEvent.getIdempotencyKey(), withdrawFailedDtoEvent);
    } catch (InsufficientWalletBalanceException e) {
      WithdrawFailedDtoEvent withdrawFailedDtoEvent =
          buildFailureEvent(withdrawDtoEvent, e.getMessage(), 4);
      saveFailedOutboxEvent(
          "withdraw", withdrawFailedDtoEvent.getIdempotencyKey(), withdrawFailedDtoEvent);
    } catch (TransientDataAccessException e) {
      System.out.println(
          "Transient DB failure on withdraw, will retry: {}"
              + withdrawDtoEvent.getIdempotencyKey()
              + e.getMessage());
      throw e;
    } catch (DataAccessException e) {
      WithdrawFailedDtoEvent withdrawFailedDtoEvent =
          buildFailureEvent(withdrawDtoEvent, "something wrong is happen", 5);
      saveFailedOutboxEvent(
          "withdraw", withdrawFailedDtoEvent.getIdempotencyKey(), withdrawFailedDtoEvent);
      throw e;
    }
  }

  private void saveFailedOutboxEvent(
      String eventType, String aggregateId, WithdrawFailedDtoEvent payload) {
    byte[] payloadBytes = convertPayloadToBye(payload);
    saveOutBox(eventType, aggregateId, payloadBytes, failedTopic);
  }

  private void saveSuccessOutboxEvent(
      String eventType, String aggregateId, WithdrawSuccessDtoEvent payload) {
    byte[] payloadBytes = convertPayloadToBye(payload);
    saveOutBox(eventType, aggregateId, payloadBytes, successTopic);
  }

  private void saveOutBox(String eventType, String aggregateId, byte[] payload, String topic) {
    OutBox outboxEvent = new OutBox();
    outboxEvent.setAggregateType(aggregateType);
    outboxEvent.setAggregateId(aggregateId);
    outboxEvent.setEventType(eventType);
    outboxEvent.setTopic(topic);
    outboxEvent.setPayload(payload);
    outBoxRepository.save(outboxEvent);
  }

  private byte[] convertPayloadToBye(SpecificRecordBase payload) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      DatumWriter<SpecificRecordBase> writer = new SpecificDatumWriter<>(payload.getSchema());
      BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
      writer.write(payload, encoder);
      encoder.flush();
      return out.toByteArray();
    } catch (IOException e) {
      // Don't swallow — a payload that can't be serialized should never
      // silently become an empty-byte row in the outbox.
      //      throw new OutboxSerializationException("Failed to serialize outbox payload for event",
      // e);
      System.out.println("e.getMessage() = " + e.getMessage());
    }
    byte[] bytes = new byte[0];
    return bytes;
  }

  private byte[] convertPayloadToBye(Object payload) {
    byte[] bytedPayload = new byte[0];
    try {
      bytedPayload = objectMapper.writeValueAsBytes(payload);
    } catch (JsonProcessingException ignored) {
      System.out.println("parsing object problem: " + ignored.getMessage());
    }
    return bytedPayload;
  }

  private WithdrawSuccessDtoEvent buildSuccessEvent(WithdrawDtoEvent event) {
    return WithdrawSuccessDtoEvent.newBuilder()
        .setUserId(event.getUserId())
        .setCurrencyId(event.getCurrencyId())
        .setAmount(event.getAmount())
        .setIdempotencyKey(event.getIdempotencyKey())
        .build();
  }

  private WithdrawFailedDtoEvent buildFailureEvent(
      WithdrawDtoEvent event, String reason, int failedEnum) {
    return WithdrawFailedDtoEvent.newBuilder()
        .setUserId(event.getUserId())
        .setCurrencyId(event.getCurrencyId())
        .setIdempotencyKey(event.getIdempotencyKey())
        .setAmount(event.getAmount())
        .setFailedReason(reason)
        .setFailedEnum(failedEnum)
        .build();
  }
}
