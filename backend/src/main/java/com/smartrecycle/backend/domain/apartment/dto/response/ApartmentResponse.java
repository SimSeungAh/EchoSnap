package com.smartrecycle.backend.domain.apartment.dto.response;

import com.smartrecycle.backend.domain.apartment.entity.Apartment;
import com.smartrecycle.backend.domain.apartment.entity.ApartmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ApartmentResponse (
        Long id,
        String name,
        String roadAddress,
        String jibunAddress,
        String buildingManagementNumber,
        BigDecimal latitude,
        BigDecimal longitude,
        ApartmentStatus status,
        Long registeredById,
        String registeredByNickname,
        String rejectionReason,
        LocalDateTime approvedAt,
        LocalDateTime createAt,
        LocalDateTime updateAt
) {
    public static ApartmentResponse from(Apartment apartment) {
        return new ApartmentResponse(
                apartment.getId(),
                apartment.getName(),
                apartment.getRoadAddress(),
                apartment.getJibunAddress(),
                apartment.getBuildingManagementNumber(),
                apartment.getLatitude(),
                apartment.getLongitude(),
                apartment.getStatus(),
                apartment.getRegisteredBy().getId(),
                apartment.getRegisteredBy().getNickname(),
                apartment.getRejectionReason(),
                apartment.getApprovedAt(),
                apartment.getCreatedAt(),
                apartment.getUpdatedAt()
        );
    }
}
