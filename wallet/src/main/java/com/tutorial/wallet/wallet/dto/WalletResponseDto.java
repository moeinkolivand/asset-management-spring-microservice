package com.tutorial.wallet.wallet.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletResponseDto(
        String name,
        Long id,
        BigDecimal balance,
        Long currencyId,
        Instant createdAt
) {
}
