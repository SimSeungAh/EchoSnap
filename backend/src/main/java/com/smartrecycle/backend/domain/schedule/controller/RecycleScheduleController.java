package com.smartrecycle.backend.domain.schedule.controller;

import com.smartrecycle.backend.domain.schedule.dto.response.ApartmentScheduleResponse;
import com.smartrecycle.backend.domain.schedule.dto.response.WasteItemScheduleResponse;
import com.smartrecycle.backend.domain.schedule.service.RecycleScheduleQueryService;
import com.smartrecycle.backend.global.response.ApiResponse;
import com.smartrecycle.backend.global.security.service.CustomUserDetails;
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
        description = "사용자용 아파트별 배출 일정 API"
)
public class RecycleScheduleController {

    private final RecycleScheduleQueryService
            recycleScheduleQueryService;

    /**
     * 로그인 사용자의 거주 아파트에 등록된
     * 전체 공식 배출 일정을 조회합니다.
     *
     * 일정은 폐기물 품목별로 묶어서 반환하며,
     * 오늘 배출 가능 여부와 다음 배출일을 함께 계산합니다.
     */
    @GetMapping("/me")
    @Operation(
            summary = "내 아파트 배출 일정 조회",
            description = """
                    로그인 사용자가 설정한 거주 아파트의
                    전체 공식 배출 일정을 조회합니다.

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
     */
    @GetMapping("/me/items/{wasteItemId}")
    @Operation(
            summary = "내 아파트 품목별 배출 일정 조회",
            description = """
                    로그인 사용자가 설정한 거주 아파트를 기준으로
                    특정 폐기물 품목의 공식 배출 일정을 조회합니다.

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
}