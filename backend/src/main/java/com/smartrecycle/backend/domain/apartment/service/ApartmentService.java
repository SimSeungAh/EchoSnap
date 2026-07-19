package com.smartrecycle.backend.domain.apartment.service;

import com.smartrecycle.backend.domain.apartment.dto.request.CreateApartmentRequest;
import com.smartrecycle.backend.domain.apartment.dto.request.RejectApartmentRequest;
import com.smartrecycle.backend.domain.apartment.dto.request.UpdateApartmentRequest;
import com.smartrecycle.backend.domain.apartment.dto.response.ApartmentResponse;
import com.smartrecycle.backend.domain.apartment.entity.Apartment;
import com.smartrecycle.backend.domain.apartment.entity.ApartmentStatus;
import com.smartrecycle.backend.domain.apartment.repository.ApartmentRepository;
import com.smartrecycle.backend.domain.user.entity.Role;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.repository.UserRepository;
import com.smartrecycle.backend.global.exception.CustomException;
import com.smartrecycle.backend.global.exception.ErrorCode;
import com.smartrecycle.backend.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApartmentService {

    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;

    /**
     * 일반 사용자가 신축 아파트를 임시 등록. 임시 등록된 아파트는 PENDING 상태로 생성되며 관리자가 승인하기 전까지 일반 검색 결과에 노출되지 않음
     */
    @Transactional
    public ApartmentResponse registerTemporary(
            Long userId,
            CreateApartmentRequest request
    ) {
        User user = getUser(userId);

        validateDuplicateBuildingManagementNumber(
                request.buildingManagementNumber()
        );

        Apartment apartment = Apartment.createTemporary(
                request.name().trim(),
                request.roadAddress().trim(),
                trimToNull(request.jibunAddress()),
                request.buildingManagementNumber().trim(),
                request.latitude(),
                request.longitude(),
                user
        );

        Apartment savedApartment =
                apartmentRepository.save(apartment);

        return ApartmentResponse.from(savedApartment);
    }

    /**
     * 관리자가 아파트를 직접 등록, 관리자가 직접 등록한 아파트는 별도의 검토 과정 없이 APPROVED 상태로 생성
     */
    @Transactional
    public ApartmentResponse registerApproved(
            Long adminId,
            CreateApartmentRequest request
    ) {
        User admin = getAdmin(adminId);

        validateDuplicateBuildingManagementNumber(
                request.buildingManagementNumber()
        );

        Apartment apartment = Apartment.createApproved(
                request.name().trim(),
                request.roadAddress().trim(),
                trimToNull(request.jibunAddress()),
                request.buildingManagementNumber().trim(),
                request.latitude(),
                request.longitude(),
                admin
        );

        Apartment savedApartment =
                apartmentRepository.save(apartment);

        return ApartmentResponse.from(savedApartment);
    }

    /**
     * 일반 사용자가 선택할 수 있는 승인 완료 아파트를 검색, APPROVED 상태의 아파트만 반환
     */
    public PageResponse<ApartmentResponse> searchApprovedApartments(
            String keyword,
            Pageable pageable
    ) {
        Page<Apartment> apartmentPage =
                apartmentRepository.searchByStatusAndKeyword(
                        ApartmentStatus.APPROVED,
                        normalizeKeyword(keyword),
                        pageable
                );

        return PageResponse.from(
                apartmentPage,
                ApartmentResponse::from
        );
    }

    /**
     * 일반 사용자가 승인 완료 아파트의 상세 정보를 조회, 승인 대기 또는 거절 상태의 아파트는 조회할 수 없음
     */
    public ApartmentResponse getApprovedApartment(
            Long apartmentId
    ) {
        Apartment apartment = getApartment(apartmentId);

        validateApprovedStatus(apartment);

        return ApartmentResponse.from(apartment);
    }

    /**
     * 관리자가 상태별 아파트 목록을 검색합니다.
     * PENDING, APPROVED, REJECTED 중 하나의 상태로 목록을 조회할 수 있음
     */
    public PageResponse<ApartmentResponse> searchApartmentsForAdmin(
            Long adminId,
            ApartmentStatus status,
            String keyword,
            Pageable pageable
    ) {
        getAdmin(adminId);

        ApartmentStatus searchStatus =
                status == null
                        ? ApartmentStatus.PENDING
                        : status;

        Page<Apartment> apartmentPage =
                apartmentRepository.searchByStatusAndKeyword(
                        searchStatus,
                        normalizeKeyword(keyword),
                        pageable
                );

        return PageResponse.from(
                apartmentPage,
                ApartmentResponse::from
        );
    }

    /**
     * 관리자가 승인 상태와 관계없이 아파트 상세 정보를 조회
     */
    public ApartmentResponse getApartmentForAdmin(
            Long adminId,
            Long apartmentId
    ) {
        getAdmin(adminId);

        Apartment apartment = getApartment(apartmentId);

        return ApartmentResponse.from(apartment);
    }

    /**
     * 관리자가 아파트 정보를 수정
     */
    @Transactional
    public ApartmentResponse updateApartment(
            Long adminId,
            Long apartmentId,
            UpdateApartmentRequest request
    ) {
        getAdmin(adminId);

        Apartment apartment = getApartment(apartmentId);

        validateDuplicateBuildingManagementNumberForUpdate(
                apartmentId,
                request.buildingManagementNumber()
        );

        apartment.update(
                request.name().trim(),
                request.roadAddress().trim(),
                trimToNull(request.jibunAddress()),
                request.buildingManagementNumber().trim(),
                request.latitude(),
                request.longitude()
        );

        return ApartmentResponse.from(apartment);
    }

    /**
     * 관리자가 승인 대기 중인 아파트를 승인
     */
    @Transactional
    public ApartmentResponse approveApartment(
            Long adminId,
            Long apartmentId
    ) {
        getAdmin(adminId);

        Apartment apartment = getApartment(apartmentId);

        validatePendingStatus(apartment);

        apartment.approve();

        return ApartmentResponse.from(apartment);
    }

    /**
     * 관리자가 승인 대기 중인 아파트를 거절
     */
    @Transactional
    public ApartmentResponse rejectApartment(
            Long adminId,
            Long apartmentId,
            RejectApartmentRequest request
    ) {
        getAdmin(adminId);

        Apartment apartment = getApartment(apartmentId);

        validatePendingStatus(apartment);

        apartment.reject(
                request.rejectionReason().trim()
        );

        return ApartmentResponse.from(apartment);
    }

    /**
     * 사용자 ID로 사용자를 조회
     */
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(
                        () -> new CustomException(
                                ErrorCode.USER_NOT_FOUND
                        )
                );
    }

    /**
     * 관리자 사용자를 조회하고 관리자 권한을 확인
     */
    private User getAdmin(Long adminId) {
        User user = getUser(adminId);

        if (user.getRole() != Role.ADMIN) {
            throw new CustomException(
                    ErrorCode.FORBIDDEN
            );
        }

        return user;
    }

    /**
     * 아파트 ID로 아파트를 조회
     */
    private Apartment getApartment(Long apartmentId) {
        return apartmentRepository.findById(apartmentId)
                .orElseThrow(
                        () -> new CustomException(
                                ErrorCode.APARTMENT_NOT_FOUND
                        )
                );
    }

    /**
     * 신규 등록 시 건물관리번호 중복을 확인합니다.
     */
    private void validateDuplicateBuildingManagementNumber(
            String buildingManagementNumber
    ) {
        boolean alreadyExists =
                apartmentRepository
                        .existsByBuildingManagementNumber(
                                buildingManagementNumber.trim()
                        );

        if (alreadyExists) {
            throw new CustomException(
                    ErrorCode.APARTMENT_ALREADY_EXISTS
            );
        }
    }

    /**
     * 수정 시 현재 아파트를 제외한 건물관리번호 중복을 확인
     */
    private void validateDuplicateBuildingManagementNumberForUpdate(
            Long apartmentId,
            String buildingManagementNumber
    ) {
        apartmentRepository
                .findByBuildingManagementNumber(
                        buildingManagementNumber.trim()
                )
                .filter(
                        apartment ->
                                !apartment.getId().equals(apartmentId)
                )
                .ifPresent(
                        apartment -> {
                            throw new CustomException(
                                    ErrorCode.APARTMENT_ALREADY_EXISTS
                            );
                        }
                );
    }

    /**
     * 승인이나 거절은 PENDING 상태에서만 가능
     */
    private void validatePendingStatus(
            Apartment apartment
    ) {
        if (apartment.getStatus()
                != ApartmentStatus.PENDING) {
            throw new CustomException(
                    ErrorCode.INVALID_APARTMENT_STATUS
            );
        }
    }

    /**
     * 일반 사용자는 APPROVED 상태인 아파트만 조회하거나 선택할 수 있음
     */
    private void validateApprovedStatus(
            Apartment apartment
    ) {
        if (apartment.getStatus()
                != ApartmentStatus.APPROVED) {
            throw new CustomException(
                    ErrorCode.APARTMENT_NOT_APPROVED
            );
        }
    }

    /**
     * null 또는 공백 문자열은 null로 변환합니다.
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();

        return trimmedValue.isEmpty()
                ? null
                : trimmedValue;
    }

    /**
     * 검색어가 없으면 빈 문자열로 변환하여 전체 검색이 가능하게 합니다.
     */
    private String normalizeKeyword(String keyword) {
        return keyword == null
                ? ""
                : keyword.trim();
    }
}