package com.smartrecycle.backend.domain.user.service;

import com.smartrecycle.backend.domain.apartment.entity.Apartment;
import com.smartrecycle.backend.domain.apartment.entity.ApartmentStatus;
import com.smartrecycle.backend.domain.apartment.repository.ApartmentRepository;
import com.smartrecycle.backend.domain.user.dto.request.UpdateOnboardingRequest;
import com.smartrecycle.backend.domain.user.dto.request.UpdateUserApartmentRequest;
import com.smartrecycle.backend.domain.user.dto.request.UpdateUserRequest;
import com.smartrecycle.backend.domain.user.dto.request.UpdateUserSettingsRequest;
import com.smartrecycle.backend.domain.user.dto.response.UserResponse;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.repository.UserRepository;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;

    /**
     * 현재 로그인한 사용자의 정보를 조회
     */
    public UserResponse getMyInfo(
            Long userId
    ) {
        User user = getUser(userId);

        return UserResponse.from(user);
    }

    /**
     * 현재 로그인한 사용자의 닉네임을 수정
     */
    @Transactional
    public UserResponse updateMyInfo(
            Long userId,
            UpdateUserRequest request
    ) {
        User user = getUser(userId);

        user.updateNickname(
                request.nickname()
        );

        return UserResponse.from(user);
    }

    /**
     * 알림 수신 동의와 위치 정보 이용 동의 설정을 변경
     */
    @Transactional
    public UserResponse updateSettings(
            Long userId,
            UpdateUserSettingsRequest request
    ) {
        User user = getUser(userId);

        user.updateSettings(
                request.notificationEnabled(),
                request.locationEnabled()
        );

        return UserResponse.from(user);
    }

    /**
     * 모바일 앱 초기 설정 완료 여부를 변경
     */
    @Transactional
    public UserResponse updateOnboarding(
            Long userId,
            UpdateOnboardingRequest request
    ) {
        User user = getUser(userId);

        user.updateOnboardingCompleted(
                request.completed()
        );

        return UserResponse.from(user);
    }

    /**
     * 현재 로그인한 사용자의 거주 아파트를 설정하거나 변경
     * 일반 사용자는 관리자가 승인한 아파트만 선택할 수 있음
     */
    @Transactional
    public UserResponse updateApartment(
            Long userId,
            UpdateUserApartmentRequest request
    ) {
        User user = getUser(userId);

        Apartment apartment = apartmentRepository.findById(
                        request.apartmentId()
                )
                .orElseThrow(
                        () -> new CustomException(
                                ErrorCode.APARTMENT_NOT_FOUND
                        )
                );

        if (apartment.getStatus() != ApartmentStatus.APPROVED) {
            throw new CustomException(
                    ErrorCode.APARTMENT_NOT_APPROVED
            );
        }

        user.changeApartment(apartment);

        return UserResponse.from(user);
    }

    /**
     * 사용자 ID로 사용자를 조회
     */
    private User getUser(
            Long userId
    ) {
        return userRepository.findById(userId)
                .orElseThrow(
                        () -> new CustomException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );
    }
}