package com.tutorial.transaction.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositDto(
        @NotBlank(message = "the currency name is required")
        String currencyName,

        @NotBlank(message = "the amount field is required")
        @Positive(message = "the amount must be positive or greater than zero")
        BigDecimal amount,

        @NotBlank(message = "idempotencyKey is required")
        UUID idempotencyKey
) {
}
