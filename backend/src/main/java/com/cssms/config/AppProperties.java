package com.cssms.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Security security, Admin admin, Storage storage, Company company) {

    public record Company(String name, String address, String phone, String gstin) {
    }

    public record Security(
            String jwtSecret,
            int accessTokenMinutes,
            int refreshTokenDays,
            int resetTokenMinutes,
            boolean exposeResetToken,
            List<String> allowedOrigins) {
    }

    public record Admin(String username, String password, String email) {
    }

    public record Storage(String uploadDir) {
    }
}
