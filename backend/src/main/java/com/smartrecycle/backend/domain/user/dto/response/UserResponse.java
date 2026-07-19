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
        boolean notificationEnabled,
        boolean locationEnabled,
        boolean onboardingCompleted

) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getStatus(),
                user.isNotificationEnabled(),
                user.isLocationEnabled(),
                user.isOnboardingCompleted()
        );
    }
}