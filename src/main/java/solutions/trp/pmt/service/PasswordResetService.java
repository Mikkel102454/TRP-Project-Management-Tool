package solutions.trp.pmt.service;

import org.springframework.stereotype.Service;
import solutions.trp.pmt.controller.api.execption.BadRequestException;
import solutions.trp.pmt.datasource.password_reset.PasswordResetTokenEntity;
import solutions.trp.pmt.datasource.password_reset.PasswordResetTokenRepository;
import solutions.trp.pmt.datasource.users.UserEntity;
import solutions.trp.pmt.datasource.users.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {
    private static final int TOKEN_BYTES = 32;
    private static final int EXPIRATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final MailService mailService;
    private final UserService userService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            MailService mailService,
            UserService userService
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailService = mailService;
        this.userService = userService;
    }

    public void requestReset(String email, String resetPageUrl) {
        if(email == null || email.isBlank()) {
            return;
        }

        userRepository.findByEmail(email.trim().toLowerCase()).ifPresent(user -> {
            String userEmail = user.getEmail();
            if(userEmail == null || userEmail.isBlank()) {
                return;
            }

            String token = generateToken();
            PasswordResetTokenEntity tokenEntity = new PasswordResetTokenEntity();
            tokenEntity.setTokenHash(hashToken(token));
            tokenEntity.setUser(user);
            tokenEntity.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES));
            tokenRepository.save(tokenEntity);

            mailService.sendPasswordReset(userEmail, resetPageUrl + "?token=" + token);
        });
    }

    public void confirmReset(String token, String newPassword) {
        if(newPassword == null || newPassword.length() < 3) {
            throw new BadRequestException("Password must be at least 3 characters");
        }

        PasswordResetTokenEntity tokenEntity = tokenRepository.findByTokenHash(hashToken(token))
                .orElseThrow(() -> new BadRequestException("Password reset link is invalid or expired"));

        LocalDateTime now = LocalDateTime.now();
        if(tokenEntity.getUsedAt() != null || tokenEntity.getExpiresAt().isBefore(now)) {
            throw new BadRequestException("Password reset link is invalid or expired");
        }

        UserEntity user = tokenEntity.getUser();
        userService.updatePassword(user, newPassword);
        tokenEntity.setUsedAt(now);
        tokenRepository.save(tokenEntity);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash password reset token", ex);
        }
    }
}
