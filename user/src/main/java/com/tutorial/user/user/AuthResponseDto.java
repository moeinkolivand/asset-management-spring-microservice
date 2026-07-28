package com.tutorial.user.user;

public record AuthResponseDto(
        String token,
        String username,
        String role
) { }

