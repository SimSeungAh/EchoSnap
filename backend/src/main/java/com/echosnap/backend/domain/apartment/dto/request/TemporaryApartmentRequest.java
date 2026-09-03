package com.echosnap.backend.domain.apartment.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TemporaryApartmentRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 255) String roadAddress,
    @Size(max = 255) String jibunAddress,
    String buildingManagementNumber,
    @Digits(integer = 2, fraction = 7)
    @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
    @Digits(integer = 3, fraction = 7)
    @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude
) {
}
