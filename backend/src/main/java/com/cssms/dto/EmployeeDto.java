package com.cssms.dto;

import com.cssms.enums.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class EmployeeDto {

    private EmployeeDto() {
    }

    public record Request(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits") String mobileNumber,
            @Email @Size(max = 150) String email,
            @Size(max = 500) String address,
            @NotNull LocalDate joiningDate,
            @NotNull @PositiveOrZero BigDecimal salary,
            @Size(max = 100) String designation,
            Status status) {
    }

    public record Response(
            Long id,
            String employeeCode,
            String fullName,
            String mobileNumber,
            String email,
            String address,
            LocalDate joiningDate,
            BigDecimal salary,
            String designation,
            Status status) {
    }
}
