package com.smartrecycle.backend.domain.user.dto.admin;

import com.smartrecycle.backend.domain.apartment.entity.Apartment;
import com.smartrecycle.backend.domain.residence.entity.Residence;
import com.smartrecycle.backend.domain.user.entity.ResidenceType;
import com.smartrecycle.backend.domain.user.entity.Role;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class AdminUserDtos {

  private AdminUserDtos() {
  }

  public record UserResponse(
      Long id,
      String email,
      String name,
      Role role,
      UserStatus status,
      ResidenceType residenceType,
      String residenceName,
      String address,
      boolean notificationEnabled,
      boolean locationEnabled,
      boolean onboardingCompleted,
      LocalDateTime createdAt,
      LocalDateTime updatedAt
  ) {

    public static UserResponse from(
        User user
    ) {
      String residenceName = null;
      String address = null;

      if (
          user.getResidenceType()
              == ResidenceType.MANAGED_COMPLEX
      ) {
        Apartment apartment =
            user.getApartment();

        if (apartment != null) {
          residenceName =
              apartment.getName();

          address =
              apartment.getRoadAddress();
        }
      }

      if (
          user.getResidenceType()
              == ResidenceType.GENERAL_HOUSING
      ) {
        Residence residence =
            user.getResidence();

        if (residence != null) {
          residenceName =
              resolveResidenceName(
                  residence
              );

          address =
              resolveResidenceAddress(
                  residence
              );
        }
      }

      return new UserResponse(
          user.getId(),
          user.getEmail(),
          user.getNickname(),
          user.getRole(),
          user.getStatus(),
          user.getResidenceType(),
          residenceName,
          address,
          user.isNotificationEnabled(),
          user.isLocationEnabled(),
          user.isOnboardingCompleted(),
          user.getCreatedAt(),
          user.getUpdatedAt()
      );
    }

    private static String resolveResidenceName(
        Residence residence
    ) {
      if (
          residence.getBuildingName() != null
              && !residence
              .getBuildingName()
              .isBlank()
      ) {
        return residence.getBuildingName();
      }

      if (
          residence.getAdministrativeDong() != null
              && !residence
              .getAdministrativeDong()
              .isBlank()
      ) {
        return residence
            .getAdministrativeDong()
            + " 일반주택";
      }

      return "일반주택";
    }

    private static String resolveResidenceAddress(
        Residence residence
    ) {
      if (
          residence.getRoadAddress() != null
              && !residence
              .getRoadAddress()
              .isBlank()
      ) {
        return residence.getRoadAddress();
      }

      if (
          residence.getAddressName() != null
              && !residence
              .getAddressName()
              .isBlank()
      ) {
        return residence.getAddressName();
      }

      return residence.getJibunAddress();
    }
  }

  public record ChangeStatusRequest(
      @NotNull
      UserStatus status
  ) {
  }

  public record ChangeRoleRequest(
      @NotNull
      Role role
  ) {
  }
}