package com.smartrecycle.backend.domain.user.entity;

import com.smartrecycle.backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true,length = 50)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private String nickname;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'ACTIVE'")
  private UserStatus status;

  @Column(nullable = false, columnDefinition = "boolean default false")
  private boolean notificationEnabled;

  @Column(nullable = false, columnDefinition = "boolean default false")
  private boolean locationEnabled;

  @Column(nullable = false, columnDefinition = "boolean default false")
  private boolean onboardingCompleted;

  public User(String email, String password, String nickname){
    this.email = email;
    this.password = password;
    this.nickname = nickname;
    this.role = Role.USER;
    this.status = UserStatus.ACTIVE;
    this.notificationEnabled = false;
    this.locationEnabled = false;
    this.onboardingCompleted = false;
  }

  public void updateNickname(String nickname){
    this.nickname = nickname;
  }

  public void updateSettings(boolean notificationEnabled, boolean locationEnabled){
    this.notificationEnabled = notificationEnabled;
    this.locationEnabled = locationEnabled;
  }

  public void updateOnboardingCompleted(boolean onboardingCompleted){
    this.onboardingCompleted = onboardingCompleted;
  }

  public void changeStatus(UserStatus status){
    this.status = status;
  }

  @PrePersist
  private void initializeDefaults(){
    if(role == null) role = Role.USER;

    if(status == null) status = UserStatus.ACTIVE;
  }


}