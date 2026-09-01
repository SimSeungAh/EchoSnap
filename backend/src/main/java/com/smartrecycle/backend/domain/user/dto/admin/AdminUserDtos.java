package com.smartrecycle.backend.domain.user.dto.admin;

import com.smartrecycle.backend.domain.residence.entity.Residence;
import com.smartrecycle.backend.domain.user.entity.ResidenceType;
import com.smartrecycle.backend.domain.user.entity.Role;
import com.smartrecycle.backend.domain.user.entity.User;
import com.smartrecycle.backend.domain.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

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
      String createdAt
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
          resolveResidenceName(user),
          resolveAddress(user),
          user.getCreatedAt() == null
              ? null
              : user.getCreatedAt().toString()
      );
    }

    private static String resolveResidenceName(
        User user
    ) {
      if (
          user.getResidenceType()
              == ResidenceType.MANAGED_COMPLEX
              && user.getApartment() != null
      ) {
        return textOrDefault(
            user.getApartment().getName(),
            "공동주택"
        );
      }

      if (
          user.getResidenceType()
              == ResidenceType.GENERAL_HOUSING
              && user.getResidence() != null
      ) {
        Residence residence =
            user.getResidence();

        if (hasText(residence.getBuildingName())) {
          return residence
              .getBuildingName()
              .trim();
        }

        return "일반주택";
      }

      return "미설정";
    }

    private static String resolveAddress(
        User user
    ) {
      if (
          user.getResidenceType()
              == ResidenceType.MANAGED_COMPLEX
              && user.getApartment() != null
      ) {
        if (
            hasText(
                user.getApartment()
                    .getRoadAddress()
            )
        ) {
          return user.getApartment()
              .getRoadAddress()
              .trim();
        }

        if (
            hasText(
                user.getApartment()
                    .getJibunAddress()
            )
        ) {
          return user.getApartment()
              .getJibunAddress()
              .trim();
        }

        return "-";
      }

      if (
          user.getResidenceType()
              == ResidenceType.GENERAL_HOUSING
              && user.getResidence() != null
      ) {
        Residence residence =
            user.getResidence();

        if (hasText(residence.getRoadAddress())) {
          return residence
              .getRoadAddress()
              .trim();
        }

        if (hasText(residence.getJibunAddress())) {
          return residence
              .getJibunAddress()
              .trim();
        }

        if (hasText(residence.getAddressName())) {
          return residence
              .getAddressName()
              .trim();
        }

        return "-";
      }

      return "-";
    }

    private static String textOrDefault(
        String value,
        String defaultValue
    ) {
      if (!hasText(value)) {
        return defaultValue;
      }

      return value.trim();
    }

    private static boolean hasText(
        String value
    ) {
      return value != null
          && !value.isBlank();
    }
  }

  public record UpdateStatusRequest(
      @NotNull
      UserStatus status
  ) {
  }

  public record UpdateRoleRequest(
      @NotNull
      Role role
  ) {
  }
}
