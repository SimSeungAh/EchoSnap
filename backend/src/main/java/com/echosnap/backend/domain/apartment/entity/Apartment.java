package com.echosnap.backend.domain.apartment.entity;

import com.echosnap.backend.domain.user.entity.User;
import com.echosnap.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "apartments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_apartments_building_management_number",
                        columnNames = "building_management_number"
                )
        },
        indexes = {
                @Index(
                        name = "idx_apartments_name",
                        columnList = "name"
                ),
                @Index(
                        name = "idx_apartments_status",
                        columnList = "status"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Apartment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 아파트 이름
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 도로명 주소
     */
    @Column(
            name = "road_address",
            nullable = false,
            length = 255
    )
    private String roadAddress;

    /**
     * 지번 주소
     */
    @Column(
            name = "jibun_address",
            length = 255
    )
    private String jibunAddress;

    /**
     * 건물관리번호
     *
     * 같은 건물을 중복 등록하지 않도록 UNIQUE 제약조건을 적용합니다.
     */
    @Column(
            name = "building_management_number",
            nullable = false,
            length = 25
    )
    private String buildingManagementNumber;

    /**
     * 위도
     */
    @Column(
            precision = 10,
            scale = 7
    )
    private BigDecimal latitude;

    /**
     * 경도
     */
    @Column(
            precision = 10,
            scale = 7
    )
    private BigDecimal longitude;

    /**
     * 등록 승인 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApartmentStatus status;

    /**
     * 아파트를 등록한 사용자
     * 일반 사용자가 신축 아파트를 임시 등록한 경우와
     * 관리자가 직접 등록한 경우를 모두 기록합니다.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "registered_by",
            nullable = false
    )
    private User registeredBy;

    /**
     * 관리자 거절 사유
     */
    @Column(
            name = "rejection_reason",
            length = 500
    )
    private String rejectionReason;

    /**
     * 관리자 승인 일시
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    private Apartment(
            String name,
            String roadAddress,
            String jibunAddress,
            String buildingManagementNumber,
            BigDecimal latitude,
            BigDecimal longitude,
            ApartmentStatus status,
            User registeredBy,
            LocalDateTime approvedAt
    ) {
        this.name = name;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.buildingManagementNumber = buildingManagementNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.registeredBy = registeredBy;
        this.approvedAt = approvedAt;
    }

    /**
     * 일반 사용자가 신축 아파트를 임시 등록할 때 사용
     */
    public static Apartment createTemporary(
            String name,
            String roadAddress,
            String jibunAddress,
            String buildingManagementNumber,
            BigDecimal latitude,
            BigDecimal longitude,
            User registeredBy
    ) {
        return new Apartment(
                name,
                roadAddress,
                jibunAddress,
                buildingManagementNumber,
                latitude,
                longitude,
                ApartmentStatus.PENDING,
                registeredBy,
                null
        );
    }

    /**
     * 관리자가 아파트를 직접 등록할 때 사용합니다.
     *
     * 관리자 직접 등록은 별도의 승인 과정 없이 APPROVED 상태로 생성합니다.
     */
    public static Apartment createApproved(
            String name,
            String roadAddress,
            String jibunAddress,
            String buildingManagementNumber,
            BigDecimal latitude,
            BigDecimal longitude,
            User registeredBy
    ) {
        return new Apartment(
                name,
                roadAddress,
                jibunAddress,
                buildingManagementNumber,
                latitude,
                longitude,
                ApartmentStatus.APPROVED,
                registeredBy,
                LocalDateTime.now()
        );
    }

    /**
     * 관리자가 아파트 정보를 수정할 때 사용합니다.
     */
    public void update(
            String name,
            String roadAddress,
            String jibunAddress,
            String buildingManagementNumber,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        this.name = name;
        this.roadAddress = roadAddress;
        this.jibunAddress = jibunAddress;
        this.buildingManagementNumber = buildingManagementNumber;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * 임시 등록된 아파트를 승인합니다.
     */
    public void approve() {
        this.status = ApartmentStatus.APPROVED;
        this.rejectionReason = null;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * 임시 등록된 아파트를 거절합니다.
     */
    public void reject(String rejectionReason) {
        this.status = ApartmentStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.approvedAt = null;
    }

    /**
     * 상태가 지정되지 않은 채 저장되는 것을 방지합니다.
     */
    @PrePersist
    private void initializeDefaults() {
        if (status == null) {
            status = ApartmentStatus.PENDING;
        }
    }
}