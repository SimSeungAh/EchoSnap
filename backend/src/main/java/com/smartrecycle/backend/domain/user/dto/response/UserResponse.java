package com.smartrecycle.backend.domain.user.dto.response;

import com.smartrecycle.backend.domain.user.entity.Role;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.entity.UserStatus;

public record UserResponse(

        Long id,
        String email,
        String nickname,
        Role role,
        UserStatus status,

        /**
         * 사용자가 현재 선택한 거주 아파트
         *
         * 아파트를 아직 선택하지 않았다면 null로 반환됩니다.
         */
        UserApartmentResponse apartment,

        boolean notificationEnabled,
        boolean locationEnabled,
        boolean onboardingCompleted

) {

    public static UserResponse from(
            User user
    ) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getStatus(),
                UserApartmentResponse.from(
                        user.getApartment()
                ),
                user.isNotificationEnabled(),
                user.isLocationEnabled(),
                user.isOnboardingCompleted()
        );
    }
}