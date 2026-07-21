package com.smartrecycle.backend.domain.user.entity;

import com.smartrecycle.backend.domain.apartment.entity.Apartment;
import com.smartrecycle.backend.global.entity.BaseEntity;
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
   * 사용자가 거주하는 아파트
   * 회원가입 직후에는 아직 아파트를 선택하지 않았을 수 있으므로 nullable 상태로 관리
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "apartment_id")
  private Apartment apartment;

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
   * 사용자의 거주 아파트를 설정하거나 변경
   */
  public void changeApartment(
          Apartment apartment
  ) {
    this.apartment = apartment;
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