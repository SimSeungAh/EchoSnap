package com.smartrecycle.backend.domain.user.service;

import com.smartrecycle.backend.domain.apartment.entity.Apartment;
import com.smartrecycle.backend.domain.apartment.entity.ApartmentStatus;
import com.smartrecycle.backend.domain.apartment.repository.ApartmentRepository;
import com.smartrecycle.backend.domain.residence.entity.Residence;
import com.smartrecycle.backend.domain.residence.service.ResidenceCollectionAreaMatchService;
import com.smartrecycle.backend.domain.user.dto.request.UpdateOnboardingRequest;
import com.smartrecycle.backend.domain.user.dto.request.UpdateUserApartmentRequest;
import com.smartrecycle.backend.domain.user.dto.request.UpdateUserRequest;
import com.smartrecycle.backend.domain.user.dto.request.UpdateUserResidenceRequest;
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

    private final ApartmentRepository
        apartmentRepository;

    private final ResidenceCollectionAreaMatchService
        residenceCollectionAreaMatchService;

    /**
     * 현재 로그인한 사용자의 정보를 조회합니다.
     */
    public UserResponse getMyInfo(
        Long userId
    ) {
        User user =
            getUser(
                userId
            );

        return UserResponse.from(
            user
        );
    }

    /**
     * 현재 로그인한 사용자의 닉네임을 수정합니다.
     */
    @Transactional
    public UserResponse updateMyInfo(
        Long userId,
        UpdateUserRequest request
    ) {
        User user =
            getUser(
                userId
            );

        user.updateNickname(
            request.nickname()
        );

        return UserResponse.from(
            user
        );
    }

    /**
     * 알림 수신 동의와 위치 정보 이용 동의를 변경합니다.
     */
    @Transactional
    public UserResponse updateSettings(
        Long userId,
        UpdateUserSettingsRequest request
    ) {
        User user =
            getUser(
                userId
            );

        user.updateSettings(
            request.notificationEnabled(),
            request.locationEnabled()
        );

        return UserResponse.from(
            user
        );
    }

    /**
     * Flutter 앱 초기 설정 완료 여부를 변경합니다.
     */
    @Transactional
    public UserResponse updateOnboarding(
        Long userId,
        UpdateOnboardingRequest request
    ) {
        User user =
            getUser(
                userId
            );

        user.updateOnboardingCompleted(
            request.completed()
        );

        return UserResponse.from(
            user
        );
    }

    /**
     * 단지 자체 배출 일정을 사용하는 거주지를 설정하거나 변경합니다.
     *
     * 아파트, 오피스텔 등 관리주체의 자체 일정이 있는 거주지는
     * 관리자가 승인한 Apartment 데이터만 선택할 수 있습니다.
     *
     * 단지를 선택하면 사용자의 residenceType도
     * MANAGED_COMPLEX로 함께 변경됩니다.
     */
    @Transactional
    public UserResponse updateApartment(
        Long userId,
        UpdateUserApartmentRequest request
    ) {
        User user =
            getUser(
                userId
            );

        Apartment apartment =
            apartmentRepository
                .findById(
                    request.apartmentId()
                )
                .orElseThrow(
                    () ->
                        new CustomException(
                            ErrorCode.APARTMENT_NOT_FOUND
                        )
                );

        if (
            apartment.getStatus()
                != ApartmentStatus.APPROVED
        ) {
            throw new CustomException(
                ErrorCode.APARTMENT_NOT_APPROVED
            );
        }

        user.changeToManagedComplex(
            apartment
        );

        return UserResponse.from(
            user
        );
    }

    /**
     * 주소 기반 지역 수거 일정을 사용하는
     * 일반주택 거주지를 설정하거나 변경합니다.
     *
     * 단독주택 / 다가구주택 / 연립주택 /
     * 다세대주택 중 사용자가 선택한
     * 세부 주거 형태도 함께 저장합니다.
     *
     * 최초 설정이면 Residence를 생성하고,
     * 기존 주소가 있으면 같은 Residence를 갱신합니다.
     *
     * 주소를 설정하면 사용자의 residenceType은
     * GENERAL_HOUSING으로 변경되고,
     * Apartment 연결은 제거됩니다.
     *
     * 이후 주소와 세부 주거 형태를 기준으로
     * CollectionArea를 다시 매칭합니다.
     */
    @Transactional
    public UserResponse updateResidence(
        Long userId,
        UpdateUserResidenceRequest request
    ) {
        User user =
            getUser(
                userId
            );

        Residence residence =
            user.getResidence();

        if (residence == null) {

            residence =
                Residence.create(
                    request.generalHousingType(),
                    request.addressName(),
                    request.roadAddress(),
                    request.jibunAddress(),
                    request.buildingName(),
                    request.zoneNo(),
                    request.sido(),
                    request.sigungu(),
                    request.legalDong(),
                    request.administrativeDong(),
                    request.legalDongCode(),
                    request.administrativeDongCode(),
                    request.latitude(),
                    request.longitude()
                );

        } else {

            residence.update(
                request.generalHousingType(),
                request.addressName(),
                request.roadAddress(),
                request.jibunAddress(),
                request.buildingName(),
                request.zoneNo(),
                request.sido(),
                request.sigungu(),
                request.legalDong(),
                request.administrativeDong(),
                request.legalDongCode(),
                request.administrativeDongCode(),
                request.latitude(),
                request.longitude()
            );
        }

        /*
         * 먼저 일반주택 Residence를
         * 현재 사용자와 연결합니다.
         */
        user.changeToGeneralHousing(
            residence
        );

        /*
         * 저장된 주소와 세부 주거 형태를 기준으로
         * 실제 지자체 CollectionArea 후보를 찾습니다.
         *
         * 공공데이터가 동기화되어 있지 않거나
         * 매칭 결과가 애매해도
         * 주소 저장 자체는 정상 완료됩니다.
         */
        residenceCollectionAreaMatchService
            .matchAndAssign(
                residence
            );

        return UserResponse.from(
            user
        );
    }

    /**
     * 사용자 ID로 사용자를 조회합니다.
     */
    private User getUser(
        Long userId
    ) {
        return userRepository
            .findById(
                userId
            )
            .orElseThrow(
                () ->
                    new CustomException(
                        ErrorCode.USER_NOT_FOUND
                    )
            );
    }
}