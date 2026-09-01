package com.tutorial.wallet.wallet;

import com.tutorial.shared.common.avro.TransferResultEnum;
import com.tutorial.shared.common.avro.TransferFailureEnum;
import com.tutorial.shared.wallet.events.TransferDtoEvent;
import com.tutorial.shared.wallet.events.TransferResultDtoEvent;
import com.tutorial.sharedmodule.infra.KafkaTopics;
import com.tutorial.sharedmodule.infra.avro.AvroPayloadSerializer;
import com.tutorial.sharedmodule.infra.outbox.OutBox;
import com.tutorial.shared.wallet.events.WithdrawResultDtoEvent;
import com.tutorial.sharedmodule.infra.outbox.OutBoxRepository;
import com.tutorial.wallet.wallet.dto.*;
import com.tutorial.wallet.currency.CurrencyApiImpl;
import jakarta.persistence.EntityNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

  private final WalletRepository walletRepository;
  private final SystemAccountRepository systemAccountRepository;
  private final CurrencyApiImpl currencyApi;
  private final TransactionTemplate transactionTemplate;
  private final OutBoxRepository outBoxRepository;
  private final AvroPayloadSerializer avroSerializer;

  @Value("app.wallet.outbox.aggregate-type")
  private String aggregateType;

  private final String walletTransferResponse = KafkaTopics.WALLET_TRANSFER_RESPONSE;

  @Autowired
  public WalletService(
      WalletRepository walletRepository,
      SystemAccountRepository systemAccountRepository,
      CurrencyApiImpl currencyApi,
      TransactionTemplate transactionTemplate,
      OutBoxRepository outBoxRepository,
      AvroPayloadSerializer avroSerializer) {
    this.walletRepository = walletRepository;
    this.systemAccountRepository = systemAccountRepository;
    this.currencyApi = currencyApi;
    this.transactionTemplate = transactionTemplate;
    this.outBoxRepository = outBoxRepository;
    this.avroSerializer = avroSerializer;
  }

  private record TransferWallets(Wallet userWallet, Wallet systemWallet) {}

  private record LockedWallets(Wallet userWallet, Wallet systemWallet) {}

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
    Long currencyId = currencyApi.getCurrencyByName("USDT")
        .orElseThrow(() -> new EntityNotFoundException("USDT currency not found"))
        .getId();
    walletRepository.save(
        new Wallet(String.format("USDT-%d", userId), BigDecimal.ZERO, userId, currencyId));
  }

  @Transactional
  public void createSystemWallet(Long userId) {
    SystemAccount systemAccount = systemAccountRepository.findByUserId(userId)
        .orElseGet(() -> systemAccountRepository.save(new SystemAccount(userId)));
    Long currencyId = currencyApi.getCurrencyByName("USDT")
        .orElseThrow(() -> new EntityNotFoundException("USDT currency not found"))
        .getId();

    if (walletRepository.findByUserIdAndCurrencyId(systemAccount.getUserId(), currencyId).isEmpty()) {
      Wallet wallet = new Wallet("SYSTEM_USDT_WALLET", BigDecimal.valueOf(1_000_000.00),
          systemAccount.getUserId(), currencyId);
      wallet.setWalletStatus(WalletStatus.ACTIVE);
      walletRepository.save(wallet);
    }
  }

  private boolean isWalletBalanceGoesToNegative(Wallet wallet, BigDecimal amount) {
    return wallet.getBalance().subtract(amount).compareTo(BigDecimal.ZERO) < 0;
  }

  private boolean isWalletFrozen(Wallet wallet) {
    return wallet.getWalletStatus() == WalletStatus.FROZEN;
  }

  private TransferWallets resolveAndValidate(Long currencyId, Long userId) {

    Wallet userWallet =
        walletRepository
            .findByCurrencyIdAndUserId(currencyId, userId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Wallet not found for user=" + userId + ", currency=" + currencyId));

    Long systemUserId = systemAccountRepository.findFirstByOrderByIdAsc()
            .map(SystemAccount::getUserId)
            .orElseThrow(() -> new EntityNotFoundException("System account not found"));
    Wallet systemWallet =
        walletRepository
            .findByCurrencyIdAndUserId(currencyId, systemUserId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "System wallet not found for currency=" + currencyId));

    if (isWalletFrozen(userWallet)) {
      throw new ForbiddenActionOnFreezeWalletException("User wallet is frozen");
    }

    if (isWalletFrozen(systemWallet)) {
      throw new ForbiddenActionOnFreezeWalletException(
          "System wallet is frozen for currency = " + currencyId);
    }

    return new TransferWallets(userWallet, systemWallet);
  }

  public void processTransfer(TransferDtoEvent event) {
    if (outBoxRepository.existsByAggregateId(event.getIdempotencyKey())) {
      System.out.println("Event With Uuid " + event.getIdempotencyKey() + " Already Processed ");
      return;
    }
    TransferWallets wallets = null;
    BigDecimal amount = parseAndValidateAmount(event.getAmount());

    try {
      wallets = resolveAndValidate(event.getCurrencyId(), event.getUserId());

      executeTransfer(event, wallets, amount);

    } catch (ForbiddenActionOnFreezeWalletException e) {
      saveFailureOutBoxEvent(
          event,
          e.getMessage(),
          TransferFailureEnum.WALLET_FROZEN,
          wallets.userWallet.getId(),
          wallets.systemWallet.getId());

    } catch (EntityNotFoundException e) {
      saveFailureOutBoxEvent(
          event,
          e.getMessage(),
          TransferFailureEnum.WALLET_NOT_FOUND,
          null,
          null);

    } catch (InsufficientWalletBalanceException e) {
      saveFailureOutBoxEvent(
          event,
          e.getMessage(),
          TransferFailureEnum.INSUFFICIENT_BALANCE,
          wallets.userWallet.getId(),
          wallets.systemWallet.getId());
    }
  }

  private void executeTransfer(TransferDtoEvent event, TransferWallets wallets, BigDecimal amount) {

    transactionTemplate.executeWithoutResult(
        status -> {
          LockedWallets lockedWallets =
              lockWallets(wallets.userWallet().getId(), wallets.systemWallet().getId());
          switch (event.getTransferType()) {
            case WITHDRAW ->
                applyWithdraw(lockedWallets.userWallet, lockedWallets.systemWallet, amount);

            case DEPOSIT ->
                applyDeposit(lockedWallets.userWallet, lockedWallets.systemWallet, amount);
          }

          saveSuccessOutboxEvent(
              event, lockedWallets.userWallet.getId(), lockedWallets.systemWallet.getId());
        });
  }

  private void applyWithdraw(Wallet userWallet, Wallet systemWallet, BigDecimal amount) {

    if (isWalletBalanceGoesToNegative(userWallet, amount)) {
      throw new InsufficientWalletBalanceException("Insufficient wallet balance");
    }

    userWallet.setBalance(userWallet.getBalance().subtract(amount));

    systemWallet.setBalance(systemWallet.getBalance().add(amount));
  }

  private void applyDeposit(Wallet userWallet, Wallet systemWallet, BigDecimal amount) {

    if (isWalletBalanceGoesToNegative(systemWallet, amount)) {
      throw new InsufficientWalletBalanceException("System wallet has insufficient balance");
    }

    systemWallet.setBalance(systemWallet.getBalance().subtract(amount));

    userWallet.setBalance(userWallet.getBalance().add(amount));
  }

  private BigDecimal parseAndValidateAmount(String amount) {

    try {
      BigDecimal value = new BigDecimal(amount);

      if (value.signum() <= 0) {
        throw new IllegalArgumentException("Amount must be greater than zero");
      }

      return value;

    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid amount: " + amount, e);
    }
  }

  private LockedWallets lockWallets(Long userWalletId, Long systemWalletId) {

    Long firstId = Math.min(userWalletId, systemWalletId);
    Long secondId = Math.max(userWalletId, systemWalletId);

    Wallet first =
        walletRepository
            .findByIdForUpdate(firstId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));

    Wallet second =
        walletRepository
            .findByIdForUpdate(secondId)
            .orElseThrow(() -> new EntityNotFoundException("Wallet not found"));

    Wallet userWallet = first.getId().equals(userWalletId) ? first : second;

    Wallet systemWallet = first.getId().equals(systemWalletId) ? first : second;

    return new LockedWallets(userWallet, systemWallet);
  }

  private TransferResultDtoEvent buildSuccessEvent(
      TransferDtoEvent event, Long userWalletId, Long systemWalletId) {

    return TransferResultDtoEvent.newBuilder()
        .setUserId(event.getUserId())
        .setCurrencyId(event.getCurrencyId())
        .setAmount(event.getAmount())
        .setIdempotencyKey(event.getIdempotencyKey())
        .setUserWalletId(userWalletId)
        .setSystemWalletId(systemWalletId)
        .setStatus(TransferResultEnum.SUCCESS)
        .setTransferType(event.getTransferType())
        .setFailedReason(null)
        .setFailedEnum(null)
        .build();
  }

  private TransferResultDtoEvent buildFailureEvent(
      TransferDtoEvent event,
      String reason,
      TransferFailureEnum failureEnum,
      Long userWalletId,
      Long systemWalletId) {

    return TransferResultDtoEvent.newBuilder()
        .setUserId(event.getUserId())
        .setCurrencyId(event.getCurrencyId())
        .setAmount(event.getAmount())
        .setIdempotencyKey(event.getIdempotencyKey())
        .setUserWalletId(userWalletId)
        .setSystemWalletId(systemWalletId)
        .setStatus(TransferResultEnum.FAILED)
        .setTransferType(event.getTransferType())
        .setFailedReason(reason)
        .setFailedEnum(failureEnum)
        .build();
  }

  private void saveSuccessOutboxEvent(
      TransferDtoEvent event, Long userWalletId, Long systemWalletId) {

    TransferResultDtoEvent result = buildSuccessEvent(event, userWalletId, systemWalletId);

    saveOutboxEvent("transfer", event.getIdempotencyKey(), result);
  }

  private void saveFailureOutBoxEvent(
      TransferDtoEvent event,
      String reason,
      TransferFailureEnum failureEnum,
      Long userWalletId,
      Long systemWalletId) {

    TransferResultDtoEvent result =
        buildFailureEvent(event, reason, failureEnum, userWalletId, systemWalletId);

    saveOutboxEvent("transfer", event.getIdempotencyKey(), result);
  }

  private void saveOutboxEvent(
      String eventType, String aggregateId, TransferResultDtoEvent payload) {

    byte[] payloadBytes = avroSerializer.serialize(walletTransferResponse, payload);

    OutBox outbox = new OutBox();

    outbox.setAggregateType(aggregateType);
    outbox.setAggregateId(aggregateId);
    outbox.setEventType(eventType);
    outbox.setTopic(walletTransferResponse);
    outbox.setPayload(payloadBytes);
    outbox.setPartitionKey(Long.toString(payload.getUserId()));
    outBoxRepository.save(outbox);
  }
}
