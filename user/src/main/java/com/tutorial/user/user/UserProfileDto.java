package com.tutorial.user.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileDto(
        @Size(min = 3, max = 35) String name,
        @Size(min = 3, max = 35) String lastName,
        @Size(min = 11, max = 11) @Pattern(regexp = "^\\d{11}$", message = "Must be exactly 11 digits") String phoneNumber
) {
}