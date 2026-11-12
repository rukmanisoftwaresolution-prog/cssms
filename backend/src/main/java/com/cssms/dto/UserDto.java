package com.cssms.dto;

import com.cssms.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDto {

    private UserDto() {
    }

    public record Request(
            @NotBlank @Size(min = 3, max = 50)
            @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Only letters, digits, '.', '_' and '-' are allowed") String username,
            @Pattern(regexp = AuthDto.PASSWORD_REGEX, message = AuthDto.PASSWORD_MESSAGE) String password,
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 150) String email,
            @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits") String mobileNumber,
            @NotNull RoleName role,
            Boolean active) {
    }

    public record Response(
            Long id,
            String username,
            String fullName,
            String email,
            String mobileNumber,
            RoleName role,
            boolean active) {
    }
}
