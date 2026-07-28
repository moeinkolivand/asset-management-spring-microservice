package com.tutorial.user.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @Size(min = 11, max = 11) @Pattern(regexp = "^\\d{11}$", message = "Must be exactly 11 digits") String phoneNumber,
        @NotBlank(message = "password is required")
        String password
) {}
