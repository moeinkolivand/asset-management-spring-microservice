package com.tutorial.user.user;

public record UserRegisteredEvent(
        Long userId,
        String phoneNumber,
        String role
) {
}
