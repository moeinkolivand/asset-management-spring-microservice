package com.tutorial.wallet.wallet.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WalletRequestDto(
        @NotNull
        @Size(min = 3, max = 20, message = "name must be between 2 and 100 characters")
        String name,

        @NotNull
        Long currencyId
) {}
