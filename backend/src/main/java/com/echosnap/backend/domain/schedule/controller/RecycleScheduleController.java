package com.echosnap.backend.domain.schedule.controller;

import com.echosnap.backend.domain.schedule.dto.response.ApartmentScheduleResponse;
import com.echosnap.backend.domain.schedule.dto.response.GeneralHousingScheduleResponse;
import com.echosnap.backend.domain.schedule.dto.response.WasteItemScheduleResponse;
import com.echosnap.backend.domain.schedule.service.CollectionAreaScheduleQueryService;
import com.echosnap.backend.domain.schedule.service.RecycleScheduleQueryService;
import com.echosnap.backend.global.response.ApiResponse;
import com.echosnap.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Tag(
    name = "Recycle Schedule",
    description = "사용자 거주지별 배출 일정 API"
)
public class RecycleScheduleController {

    private final RecycleScheduleQueryService
        recycleScheduleQueryService;

    private final CollectionAreaScheduleQueryService
        collectionAreaScheduleQueryService;

    /**
     * 로그인 사용자의 거주 아파트에 등록된
     * 전체 공식 배출 일정을 조회합니다.
     *
     * MANAGED_COMPLEX 사용자용 API입니다.
     */
    @GetMapping("/me")
    @Operation(
        summary = "내 아파트 배출 일정 조회",
        description = """
                    로그인 사용자가 설정한 거주 아파트의
                    전체 공식 배출 일정을 조회합니다.

                    MANAGED_COMPLEX 거주 유형에서 사용합니다.

                    각 품목별로 오늘 일정 존재 여부,
                    현재 배출 가능 여부,
                    다음 배출 날짜와 시간을 함께 반환합니다.
                    """
    )
    public ApiResponse<ApartmentScheduleResponse>
    getMyApartmentSchedule(
        @AuthenticationPrincipal
        CustomUserDetails userDetails
    ) {
        ApartmentScheduleResponse response =
            recycleScheduleQueryService
                .getMyApartmentSchedule(
                    userDetails.getUserId()
                );

        return ApiResponse.success(
            "내 아파트 배출 일정 조회 성공",
            response
        );
    }

    /**
     * 로그인 사용자의 거주 아파트를 기준으로
     * 특정 폐기물 품목의 공식 일정을 조회합니다.
     *
     * MANAGED_COMPLEX 사용자용 API입니다.
     */
    @GetMapping("/me/items/{wasteItemId}")
    @Operation(
        summary = "내 아파트 품목별 배출 일정 조회",
        description = """
                    로그인 사용자가 설정한 거주 아파트를 기준으로
                    특정 폐기물 품목의 공식 배출 일정을 조회합니다.

                    MANAGED_COMPLEX 거주 유형에서 사용합니다.

                    오늘 배출 가능 여부,
                    현재 배출 가능 여부,
                    다음 배출일과 전체 요일 일정을 반환합니다.
                    """
    )
    public ApiResponse<WasteItemScheduleResponse>
    getMyWasteItemSchedule(
        @AuthenticationPrincipal
        CustomUserDetails userDetails,
        @PathVariable
        Long wasteItemId
    ) {
        WasteItemScheduleResponse response =
            recycleScheduleQueryService
                .getMyWasteItemSchedule(
                    userDetails.getUserId(),
                    wasteItemId
                );

        return ApiResponse.success(
            "품목별 배출 일정 조회 성공",
            response
        );
    }

    /**
     * 로그인한 일반주택 사용자의
     * 주소 기반 지역 배출 일정을 조회합니다.
     *
     * GENERAL_HOUSING 사용자용 API입니다.
     *
     * 생활쓰레기 / 음식물쓰레기 / 재활용품
     * 세 종류의 수거구역과 일정을 반환합니다.
     */
    @GetMapping("/me/general-housing")
    @Operation(
        summary = "내 일반주택 지역 배출 일정 조회",
        description = """
                    로그인 사용자가 설정한 일반주택 Residence를 기준으로
                    지역 생활폐기물 배출 일정을 조회합니다.

                    GENERAL_HOUSING 거주 유형에서 사용합니다.

                    Residence에 연결된 CollectionArea를 기준으로
                    생활쓰레기, 음식물쓰레기, 재활용품의
                    배출 요일과 시간, 배출 방법을 반환합니다.

                    공공데이터 일정이 20:00~02:00처럼
                    자정을 넘어가는 경우도 실제 현재 시각을 기준으로
                    배출 가능 여부를 계산합니다.

                    수거구역이 명확하게 매칭되지 않았거나
                    일정 데이터가 없는 종류도 응답에서 제외하지 않고
                    상태값과 함께 반환합니다.
                    """
    )
    public ApiResponse<GeneralHousingScheduleResponse>
    getMyGeneralHousingSchedule(
        @AuthenticationPrincipal
        CustomUserDetails userDetails
    ) {
        GeneralHousingScheduleResponse response =
            collectionAreaScheduleQueryService
                .getMyGeneralHousingSchedule(
                    userDetails.getUserId()
                );

        return ApiResponse.success(
            "내 일반주택 지역 배출 일정 조회 성공",
            response
        );
    }
}