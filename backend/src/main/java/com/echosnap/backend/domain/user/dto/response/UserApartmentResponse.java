package com.echosnap.backend.domain.user.dto.response;

import com.echosnap.backend.domain.apartment.entity.Apartment;

public record UserApartmentResponse(

        Long id,
        String name,
        String roadAddress,
        String jibunAddress

) {

    public static UserApartmentResponse from(
            Apartment apartment
    ) {
        if (apartment == null) {
            return null;
        }

        return new UserApartmentResponse(
                apartment.getId(),
                apartment.getName(),
                apartment.getRoadAddress(),
                apartment.getJibunAddress()
        );
    }
}