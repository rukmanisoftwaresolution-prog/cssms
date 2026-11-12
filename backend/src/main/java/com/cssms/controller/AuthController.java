package com.cssms.controller;

import com.cssms.dto.AuthDto.AuthResponse;
import com.cssms.dto.AuthDto.ChangePasswordRequest;
import com.cssms.dto.AuthDto.ForgotPasswordRequest;
import com.cssms.dto.AuthDto.ForgotPasswordResponse;
import com.cssms.dto.AuthDto.LoginRequest;
import com.cssms.dto.AuthDto.LogoutRequest;
import com.cssms.dto.AuthDto.RefreshRequest;
import com.cssms.dto.AuthDto.ResetPasswordRequest;
import com.cssms.dto.AuthDto.UpdateProfileRequest;
import com.cssms.dto.AuthDto.UserProfile;
import com.cssms.security.Access;
import com.cssms.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @PreAuthorize(Access.ALL_ROLES)
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize(Access.ALL_ROLES)
    public UserProfile me() {
        return authService.me();
    }

    @PutMapping("/me")
    @PreAuthorize(Access.ALL_ROLES)
    public UserProfile updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(request);
    }
}
