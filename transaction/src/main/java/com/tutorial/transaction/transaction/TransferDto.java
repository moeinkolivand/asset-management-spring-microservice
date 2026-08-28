package com.tutorial.transaction.transaction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferDto(
    @NotNull(message = "the currency name is required") Long currencyId,
    @NotNull(message = "the amount field is required")
        @Positive(message = "the amount must be positive or greater than zero")
        BigDecimal amount,
    @NotNull(message = "idempotencyKey is required") UUID idempotencyKey,
    @NotNull(message = "transfer type is required") TransactionTransferType transactionTransferType) {}
