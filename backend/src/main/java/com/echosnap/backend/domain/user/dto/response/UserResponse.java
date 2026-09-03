package com.echosnap.backend.domain.user.dto.response;

import com.echosnap.backend.domain.user.entity.ResidenceType;
import com.echosnap.backend.domain.user.entity.Role;
import com.echosnap.backend.domain.user.entity.User;
import com.echosnap.backend.domain.user.entity.UserStatus;

public record UserResponse(

    Long id,
    String email,
    String nickname,
    Role role,
    UserStatus status,

    /**
     * 사용자의 배출 일정 적용 거주지 유형
     *
     * 초기 설정 전에는 null일 수 있습니다.
     *
     * MANAGED_COMPLEX:
     * 관리주체 또는 단지 자체 배출 일정 사용
     *
     * GENERAL_HOUSING:
     * 주소 기반 지역 수거 일정 사용
     */
    ResidenceType residenceType,

    /**
     * 사용자가 현재 선택한 거주 단지
     *
     * MANAGED_COMPLEX인 경우 반환됩니다.
     *
     * GENERAL_HOUSING 또는
     * 거주지 초기 설정 전에는 null입니다.
     */
    UserApartmentResponse apartment,

    /**
     * 사용자가 현재 선택한 주소 기반 거주지
     *
     * GENERAL_HOUSING인 경우 반환됩니다.
     *
     * MANAGED_COMPLEX 또는
     * 거주지 초기 설정 전에는 null입니다.
     */
    UserResidenceResponse residence,

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
            user.getResidenceType(),
            UserApartmentResponse.from(
                user.getApartment()
            ),
            UserResidenceResponse.from(
                user.getResidence()
            ),
            user.isNotificationEnabled(),
            user.isLocationEnabled(),
            user.isOnboardingCompleted()
        );
    }
}