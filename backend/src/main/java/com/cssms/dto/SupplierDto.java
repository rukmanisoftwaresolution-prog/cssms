package com.cssms.dto;

import com.cssms.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class SupplierDto {

    public static final String GST_REGEX = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$";
    public static final String PAN_REGEX = "^[A-Z]{5}[0-9]{4}[A-Z]$";

    private SupplierDto() {
    }

    public record Request(
            @NotBlank @Size(max = 150) String supplierName,
            @Pattern(regexp = GST_REGEX, message = "Invalid GST number") String gstNumber,
            @Pattern(regexp = PAN_REGEX, message = "Invalid PAN number") String panNumber,
            @NotBlank @Size(max = 120) String contactPerson,
            @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits") String mobileNumber,
            @NotBlank @Size(max = 500) String address,
            @Size(max = 500) String bankDetails,
            Status status) {
    }

    public record Response(
            Long id,
            String supplierCode,
            String supplierName,
            String gstNumber,
            String panNumber,
            String contactPerson,
            String mobileNumber,
            String address,
            String bankDetails,
            Status status,
            BigDecimal outstandingAmount) {
    }
}
