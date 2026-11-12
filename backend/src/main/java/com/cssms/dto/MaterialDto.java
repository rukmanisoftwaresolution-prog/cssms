package com.cssms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MaterialDto {

    private MaterialDto() {
    }

    public record Request(
            @NotBlank @Size(max = 100) String materialName,
            @Size(max = 500) String description,
            @Size(max = 20) String unit) {
    }

    public record Response(
            Long id,
            String materialCode,
            String materialName,
            String description,
            String unit) {
    }
}
