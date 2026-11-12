package com.cssms.service;

import com.cssms.config.AppProperties;
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
import com.cssms.entity.PasswordResetToken;
import com.cssms.entity.RefreshToken;
import com.cssms.entity.User;
import com.cssms.exception.BadRequestException;
import com.cssms.exception.BusinessException;
import com.cssms.exception.ResourceNotFoundException;
import com.cssms.exception.TokenException;
import com.cssms.repository.PasswordResetTokenRepository;
import com.cssms.repository.RefreshTokenRepository;
import com.cssms.repository.UserRepository;
import com.cssms.security.JwtService;
import com.cssms.security.UserPrincipal;
import com.cssms.util.CurrentUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties properties;
    private final AuditLogService auditLogService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", principal.getId()));
        auditLogService.log(user.getUsername(), "LOGIN", "User", String.valueOf(user.getId()), null);
        return issueTokens(user);
    }

    @Transactional(noRollbackFor = TokenException.class)
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(request.refreshToken()))
                .orElseThrow(() -> new TokenException("Invalid refresh token"));
        if (stored.isRevoked()) {
            // A rotated token was presented again: assume theft and invalidate the whole family.
            refreshTokenRepository.revokeAllForUser(stored.getUser().getId());
            throw new TokenException("Refresh token has been revoked");
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenException("Refresh token has expired");
        }
        User user = stored.getUser();
        if (!user.isActive()) {
            throw new TokenException("Account is disabled");
        }
        stored.setRevoked(true);
        return issueTokens(user);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        if (request != null && request.refreshToken() != null) {
            refreshTokenRepository.findByTokenHash(hash(request.refreshToken())).ifPresent(token -> {
                token.setRevoked(true);
                auditLogService.log(token.getUser().getUsername(), "LOGOUT", "User",
                        String.valueOf(token.getUser().getId()), null);
            });
        }
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = currentUserEntity();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllForUser(user.getId());
        auditLogService.log(user.getUsername(), "CHANGE_PASSWORD", "User", String.valueOf(user.getId()), null);
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String message = "If the email is registered, password reset instructions have been generated";
        String exposedToken = null;
        var user = userRepository.findByEmailIgnoreCase(request.email()).filter(User::isActive);
        if (user.isPresent()) {
            String rawToken = randomToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user.get());
            token.setTokenHash(hash(rawToken));
            token.setExpiresAt(LocalDateTime.now().plusMinutes(properties.security().resetTokenMinutes()));
            resetTokenRepository.save(token);
            auditLogService.log(user.get().getUsername(), "FORGOT_PASSWORD", "User",
                    String.valueOf(user.get().getId()), null);
            if (properties.security().exposeResetToken()) {
                exposedToken = rawToken;
            } else {
                // Hook for an email/SMS gateway: deliver rawToken to the user's registered address.
                log.info("Password reset token generated for user {}", user.get().getUsername());
            }
        }
        return new ForgotPasswordResponse(message, exposedToken);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = resetTokenRepository.findByTokenHash(hash(request.token()))
                .orElseThrow(() -> new BusinessException("Invalid or expired reset token"));
        if (token.isUsed() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Invalid or expired reset token");
        }
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.setUsed(true);
        refreshTokenRepository.revokeAllForUser(user.getId());
        auditLogService.log(user.getUsername(), "RESET_PASSWORD", "User", String.valueOf(user.getId()), null);
    }

    @Transactional(readOnly = true)
    public UserProfile me() {
        return toProfile(currentUserEntity());
    }

    @Transactional
    public UserProfile updateProfile(UpdateProfileRequest request) {
        User user = currentUserEntity();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), user.getId())) {
            throw new BusinessException("Email is already used by another user");
        }
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setMobileNumber(request.mobileNumber());
        return toProfile(user);
    }

    public static UserProfile toProfile(User user) {
        return new UserProfile(user.getId(), user.getUsername(), user.getFullName(), user.getEmail(),
                user.getMobileNumber(), user.getRole().getName());
    }

    private User currentUserEntity() {
        return userRepository.findByUsernameIgnoreCase(CurrentUser.usernameOrSystem())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AuthResponse issueTokens(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        String rawRefresh = randomToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(rawRefresh));
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(properties.security().refreshTokenDays()));
        refreshTokenRepository.save(refreshToken);
        return new AuthResponse(jwtService.generateAccessToken(principal), rawRefresh, "Bearer",
                jwtService.accessTokenSeconds(), toProfile(user));
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
