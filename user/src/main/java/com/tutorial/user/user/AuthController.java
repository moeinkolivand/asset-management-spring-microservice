package com.tutorial.user.user;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "invalid phone number or password"));
        }
    }


    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> userProfile(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.userProfile(currentUser));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileDto> updateUserProfile(@AuthenticationPrincipal User currentUser, @Valid @RequestBody UserProfileDto userProfileDto) {
        return ResponseEntity.ok(authService.updateUserProfile(currentUser, userProfileDto));
    }

}