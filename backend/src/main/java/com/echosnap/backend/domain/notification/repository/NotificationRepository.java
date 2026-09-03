package com.echosnap.backend.domain.notification.repository;

import com.echosnap.backend.domain.notification.entity.Notification;
import com.echosnap.backend.domain.user.entity.ResidenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository
    extends JpaRepository<Notification, Long> {

  /**
   * 관리자 발송 이력 검색
   */
  @Query(
      value = """
                    select n
                    from Notification n
                    where (
                        :status is null
                        or n.status = :status
                    )
                    and (
                        :targetType is null
                        or n.targetType = :targetType
                    )
                    and (
                        :keyword = ''
                        or lower(n.title)
                            like lower(concat('%', :keyword, '%'))
                        or lower(n.body)
                            like lower(concat('%', :keyword, '%'))
                    )
                    order by n.sentAt desc
                    """,
      countQuery = """
                    select count(n)
                    from Notification n
                    where (
                        :status is null
                        or n.status = :status
                    )
                    and (
                        :targetType is null
                        or n.targetType = :targetType
                    )
                    and (
                        :keyword = ''
                        or lower(n.title)
                            like lower(concat('%', :keyword, '%'))
                        or lower(n.body)
                            like lower(concat('%', :keyword, '%'))
                    )
                    """
  )
  Page<Notification> searchAdmin(
      @Param("keyword")
      String keyword,

      @Param("status")
      Notification.Status status,

      @Param("targetType")
      Notification.TargetType targetType,

      Pageable pageable
  );

  /**
   * 사용자의 알림 목록
   */
  @Query(
      value = """
                    select n
                    from Notification n
                    where n.status = :sentStatus
                    and (
                        n.targetType = :allTarget

                        or (
                            n.targetType = :notificationEnabledTarget
                            and :notificationEnabled = true
                        )

                        or (
                            n.targetType = :managedComplexTarget
                            and :residenceType = :managedComplexResidence
                        )

                        or (
                            n.targetType = :generalHousingTarget
                            and :residenceType = :generalHousingResidence
                        )
                    )
                    order by n.sentAt desc
                    """,
      countQuery = """
                    select count(n)
                    from Notification n
                    where n.status = :sentStatus
                    and (
                        n.targetType = :allTarget

                        or (
                            n.targetType = :notificationEnabledTarget
                            and :notificationEnabled = true
                        )

                        or (
                            n.targetType = :managedComplexTarget
                            and :residenceType = :managedComplexResidence
                        )

                        or (
                            n.targetType = :generalHousingTarget
                            and :residenceType = :generalHousingResidence
                        )
                    )
                    """
  )
  Page<Notification> findVisibleForUser(
      @Param("notificationEnabled")
      boolean notificationEnabled,

      @Param("residenceType")
      ResidenceType residenceType,

      @Param("sentStatus")
      Notification.Status sentStatus,

      @Param("allTarget")
      Notification.TargetType allTarget,

      @Param("notificationEnabledTarget")
      Notification.TargetType notificationEnabledTarget,

      @Param("managedComplexTarget")
      Notification.TargetType managedComplexTarget,

      @Param("generalHousingTarget")
      Notification.TargetType generalHousingTarget,

      @Param("managedComplexResidence")
      ResidenceType managedComplexResidence,

      @Param("generalHousingResidence")
      ResidenceType generalHousingResidence,

      Pageable pageable
  );

  /**
   * 읽지 않은 알림 개수
   */
  @Query("""
            select count(n)
            from Notification n
            where n.status = :sentStatus
            and (
                n.targetType = :allTarget

                or (
                    n.targetType = :notificationEnabledTarget
                    and :notificationEnabled = true
                )

                or (
                    n.targetType = :managedComplexTarget
                    and :residenceType = :managedComplexResidence
                )

                or (
                    n.targetType = :generalHousingTarget
                    and :residenceType = :generalHousingResidence
                )
            )
            and :userId not member of n.readUserIds
            """)
  long countUnreadForUser(
      @Param("userId")
      Long userId,

      @Param("notificationEnabled")
      boolean notificationEnabled,

      @Param("residenceType")
      ResidenceType residenceType,

      @Param("sentStatus")
      Notification.Status sentStatus,

      @Param("allTarget")
      Notification.TargetType allTarget,

      @Param("notificationEnabledTarget")
      Notification.TargetType notificationEnabledTarget,

      @Param("managedComplexTarget")
      Notification.TargetType managedComplexTarget,

      @Param("generalHousingTarget")
      Notification.TargetType generalHousingTarget,

      @Param("managedComplexResidence")
      ResidenceType managedComplexResidence,

      @Param("generalHousingResidence")
      ResidenceType generalHousingResidence
  );

  /**
   * 전체 읽음 처리를 위해 현재 사용자에게
   * 보이는 모든 알림을 가져옵니다.
   */
  @Query("""
            select n
            from Notification n
            where n.status = :sentStatus
            and (
                n.targetType = :allTarget

                or (
                    n.targetType = :notificationEnabledTarget
                    and :notificationEnabled = true
                )

                or (
                    n.targetType = :managedComplexTarget
                    and :residenceType = :managedComplexResidence
                )

                or (
                    n.targetType = :generalHousingTarget
                    and :residenceType = :generalHousingResidence
                )
            )
            """)
  List<Notification> findAllVisibleForUser(
      @Param("notificationEnabled")
      boolean notificationEnabled,

      @Param("residenceType")
      ResidenceType residenceType,

      @Param("sentStatus")
      Notification.Status sentStatus,

      @Param("allTarget")
      Notification.TargetType allTarget,

      @Param("notificationEnabledTarget")
      Notification.TargetType notificationEnabledTarget,

      @Param("managedComplexTarget")
      Notification.TargetType managedComplexTarget,

      @Param("generalHousingTarget")
      Notification.TargetType generalHousingTarget,

      @Param("managedComplexResidence")
      ResidenceType managedComplexResidence,

      @Param("generalHousingResidence")
      ResidenceType generalHousingResidence
  );

  /**
   * 관리자 대시보드의 오늘 발송 수
   */
  long countByStatusAndSentAtGreaterThanEqualAndSentAtLessThan(
      Notification.Status status,
      LocalDateTime start,
      LocalDateTime end
  );
}