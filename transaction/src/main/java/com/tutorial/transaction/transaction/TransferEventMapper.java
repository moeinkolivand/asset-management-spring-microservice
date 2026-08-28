package com.tutorial.transaction.transaction;

import com.tutorial.shared.common.avro.TransferTypeEnumEvent;
import com.tutorial.shared.wallet.events.TransferDtoEvent;
import org.springframework.stereotype.Component;

@Component
public class TransferEventMapper {

  public TransferDtoEvent toEvent(TransferDto dto, Long userId) {

    return TransferDtoEvent.newBuilder()
        .setAmount(dto.amount().toString())
        .setIdempotencyKey(dto.idempotencyKey().toString())
        .setCurrencyId(dto.currencyId())
        .setUserId(userId)
        .setTransferType(
            dto.transactionTransferType() == TransactionTransferType.WITHDRAW
                ? TransferTypeEnumEvent.WITHDRAW
                : TransferTypeEnumEvent.DEPOSIT)
        .build();
  }
}
