package com.smartrecycle.backend.domain.schedule.entity;

import com.smartrecycle.backend.domain.apartment.entity.Apartment;
import com.smartrecycle.backend.domain.waste.entity.WasteItem;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Entity
@Table(
        name = "recycle_schedules",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recycle_schedules_apartment_item_day",
                        columnNames = {
                                "apartment_id",
                                "waste_item_id",
                                "day_of_week"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_recycle_schedules_apartment",
                        columnList = "apartment_id"
                ),
                @Index(
                        name = "idx_recycle_schedules_waste_item",
                        columnList = "waste_item_id"
                ),
                @Index(
                        name = "idx_recycle_schedules_day",
                        columnList = "day_of_week"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecycleSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 배출 일정이 적용되는 아파트
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "apartment_id",
            nullable = false
    )
    private Apartment apartment;

    /**
     * 배출 일정이 적용되는 폐기물 품목
     *
     * 같은 카테고리라도 품목마다 배출 일정이 다를 수 있으므로 WasteCategory가 아닌 WasteItem과 연결
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "waste_item_id",
            nullable = false
    )
    private WasteItem wasteItem;

    /**
     * 배출 가능한 요일
     *상시 배출 일정인 경우 null로 저장
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "day_of_week",
            length = 20
    )
    private DayOfWeek dayOfWeek;

    /**
     * 배출 시작 시간
     * 상시 배출 일정인 경우 null로 저장
     */
    @Column(name = "start_time")
    private LocalTime startTime;

    /**
     * 배출 종료 시간
     * 상시 배출 일정인 경우 null로 저장
     */
    @Column(name = "end_time")
    private LocalTime endTime;

    /**
     * 요일이나 시간에 관계없이 항상 배출 가능한지 여부
     */
    @Column(
            name = "always_available",
            nullable = false
    )
    private boolean alwaysAvailable;

    private RecycleSchedule(
            Apartment apartment,
            WasteItem wasteItem,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            boolean alwaysAvailable
    ) {
        this.apartment = apartment;
        this.wasteItem = wasteItem;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.alwaysAvailable = alwaysAvailable;
    }

    /**
     * 특정 요일과 시간에만 배출할 수 있는 일정을 생성
     */
    public static RecycleSchedule createWeekly(
            Apartment apartment,
            WasteItem wasteItem,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        return new RecycleSchedule(
                apartment,
                wasteItem,
                dayOfWeek,
                startTime,
                endTime,
                false
        );
    }

    /**
     * 요일과 시간에 관계없이 상시 배출 가능한 일정을 생성
     */
    public static RecycleSchedule createAlwaysAvailable(
            Apartment apartment,
            WasteItem wasteItem
    ) {
        return new RecycleSchedule(
                apartment,
                wasteItem,
                null,
                null,
                null,
                true
        );
    }

    /**
     * 기존 일정을 특정 요일과 시간의 일정으로 수정
     */
    public void updateWeekly(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.alwaysAvailable = false;
    }

    /**
     * 기존 일정을 상시 배출 일정으로 변경
     */
    public void updateAlwaysAvailable() {
        this.dayOfWeek = null;
        this.startTime = null;
        this.endTime = null;
        this.alwaysAvailable = true;
    }
}