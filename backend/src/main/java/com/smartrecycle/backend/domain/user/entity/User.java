package com.smartrecycle.backend.domain.user.entity;

import com.smartrecycle.backend.domain.apartment.entity.Apartment;
import com.smartrecycle.backend.domain.residence.entity.Residence;
import com.smartrecycle.backend.global.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(
            name = "idx_users_apartment",
            columnList = "apartment_id"
        ),
        @Index(
            name = "idx_users_residence",
            columnList = "residence_id"
        ),
        @Index(
            name = "idx_users_residence_type",
            columnList = "residence_type"
        )
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * 로그인 이메일
   */
  @Column(
      nullable = false,
      unique = true,
      length = 50
  )
  private String email;

  /**
   * 암호화된 비밀번호
   */
  @Column(nullable = false)
  private String password;

  /**
   * 사용자 닉네임
   */
  @Column(
      nullable = false,
      length = 30
  )
  private String nickname;

  /**
   * 사용자 권한
   */
  @Enumerated(EnumType.STRING)
  @Column(
      nullable = false,
      length = 20
  )
  private Role role;

  /**
   * 계정 상태
   */
  @Enumerated(EnumType.STRING)
  @Column(
      nullable = false,
      length = 20,
      columnDefinition = "varchar(20) default 'ACTIVE'"
  )
  private UserStatus status;

  /**
   * 사용자의 배출 일정 적용 거주지 유형
   *
   * 회원가입 직후 초기 설정을 완료하기 전까지는
   * 아직 거주 형태를 선택하지 않았을 수 있으므로 null을 허용합니다.
   */
  @Enumerated(EnumType.STRING)
  @Column(
      name = "residence_type",
      length = 30
  )
  private ResidenceType residenceType;

  /**
   * 단지 자체 배출 일정을 사용하는 사용자의 거주 단지
   *
   * MANAGED_COMPLEX인 경우 사용합니다.
   *
   * GENERAL_HOUSING 사용자는 Apartment를 사용하지 않고,
   * Residence와 이후 CollectionArea를 통해
   * 지역 배출 일정을 조회합니다.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "apartment_id")
  private Apartment apartment;

  /**
   * 주소 기반 지역 수거 일정을 사용하는
   * 일반주택 사용자의 현재 거주지입니다.
   *
   * GENERAL_HOUSING인 경우 사용합니다.
   *
   * User가 현재 사용 중인 Residence의 생명주기를 관리하며,
   * 일반주택에서 단지형 거주지로 변경할 경우
   * 이전 Residence 데이터도 함께 제거합니다.
   */
  @OneToOne(
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true
  )
  @JoinColumn(
      name = "residence_id",
      unique = true
  )
  private Residence residence;

  /**
   * 알림 수신 동의 여부
   */
  @Column(
      nullable = false,
      columnDefinition = "boolean default false"
  )
  private boolean notificationEnabled;

  /**
   * 위치 정보 이용 동의 여부
   */
  @Column(
      nullable = false,
      columnDefinition = "boolean default false"
  )
  private boolean locationEnabled;

  /**
   * 모바일 앱 초기 설정 완료 여부
   */
  @Column(
      nullable = false,
      columnDefinition = "boolean default false"
  )
  private boolean onboardingCompleted;

  public User(
      String email,
      String password,
      String nickname
  ) {
    this.email = email;
    this.password = password;
    this.nickname = nickname;
    this.role = Role.USER;
    this.status = UserStatus.ACTIVE;
    this.notificationEnabled = false;
    this.locationEnabled = false;
    this.onboardingCompleted = false;
  }

  /**
   * 닉네임 변경
   */
  public void updateNickname(
      String nickname
  ) {
    this.nickname = nickname;
  }

  /**
   * 알림 및 위치 이용 설정 변경
   */
  public void updateSettings(
      boolean notificationEnabled,
      boolean locationEnabled
  ) {
    this.notificationEnabled =
        notificationEnabled;

    this.locationEnabled =
        locationEnabled;
  }

  /**
   * 모바일 초기 설정 완료 상태 변경
   */
  public void updateOnboardingCompleted(
      boolean onboardingCompleted
  ) {
    this.onboardingCompleted =
        onboardingCompleted;
  }

  /**
   * 단지 자체 배출 일정을 사용하는 거주지로 변경합니다.
   *
   * 아파트, 오피스텔 등 관리주체의 자체 일정이 존재하는 경우
   * 승인된 Apartment 데이터를 연결합니다.
   *
   * 이전에 GENERAL_HOUSING을 사용하고 있었다면
   * 기존 주소 기반 Residence 연결을 제거합니다.
   */
  public void changeToManagedComplex(
      Apartment apartment
  ) {
    this.residenceType =
        ResidenceType.MANAGED_COMPLEX;

    this.apartment =
        apartment;

    this.residence =
        null;
  }

  /**
   * 주소 기반 지역 수거 일정을 사용하는
   * 일반주택으로 변경합니다.
   *
   * 이전에 단지형 거주지를 사용하고 있었다면
   * 기존 Apartment 연결을 제거하고,
   * 사용자가 선택한 주소 기반 Residence를 연결합니다.
   */
  public void changeToGeneralHousing(
      Residence residence
  ) {
    this.residenceType =
        ResidenceType.GENERAL_HOUSING;

    this.apartment =
        null;

    this.residence =
        residence;
  }

  /**
   * 계정 상태 변경
   */
  public void changeStatus(
      UserStatus status
  ) {
    this.status = status;
  }

  /**
   * 신규 사용자 기본값 설정
   */
  @PrePersist
  private void initializeDefaults() {
    if (role == null) {
      role = Role.USER;
    }

    if (status == null) {
      status = UserStatus.ACTIVE;
    }
  }
}