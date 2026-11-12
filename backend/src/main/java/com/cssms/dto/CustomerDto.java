package com.cssms.dto;

import com.cssms.enums.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class CustomerDto {

    private CustomerDto() {
    }

    /** creditLimit 0 means "no credit limit enforced". */
    public record Request(
            @NotBlank @Size(max = 150) String customerName,
            @Size(max = 150) String companyName,
            @Pattern(regexp = SupplierDto.GST_REGEX, message = "Invalid GST number") String gstNumber,
            @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits") String mobileNumber,
            @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 500) String address,
            @NotNull @PositiveOrZero BigDecimal creditLimit,
            Status status) {
    }

    public record Response(
            Long id,
            String customerCode,
            String customerName,
            String companyName,
            String gstNumber,
            String mobileNumber,
            String email,
            String address,
            BigDecimal creditLimit,
            BigDecimal outstandingAmount,
            Status status) {
    }
}
