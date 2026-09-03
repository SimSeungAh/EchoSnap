package com.echosnap.backend.domain.schedule.controller;

import com.echosnap.backend.domain.schedule.dto.request.CreateRecycleScheduleRequest;
import com.echosnap.backend.domain.schedule.dto.request.UpdateRecycleScheduleRequest;
import com.echosnap.backend.domain.schedule.dto.response.RecycleScheduleResponse;
import com.echosnap.backend.domain.schedule.service.RecycleScheduleAdminService;
import com.echosnap.backend.domain.schedule.service.RecycleScheduleQueryService;
import com.echosnap.backend.global.response.ApiResponse;
import com.echosnap.backend.global.security.service.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/schedules")
@RequiredArgsConstructor
@Tag(
        name = "Admin Recycle Schedule",
        description = "관리자용 아파트별 공식 배출 일정 API"
)
public class AdminRecycleScheduleController {

    private final RecycleScheduleAdminService
            recycleScheduleAdminService;

    private final RecycleScheduleQueryService
            recycleScheduleQueryService;

    /**
     * 특정 아파트에 등록된
     * 공식 배출 일정 전체를 조회합니다.
     */
    @GetMapping
    @Operation(
            summary = "관리자 아파트별 배출 일정 목록 조회",
            description = """
                    관리자가 특정 아파트에 등록된
                    전체 공식 배출 일정을 조회합니다.

                    apartmentId를 기준으로 조회하며,
                    폐기물 품목과 카테고리 정보도 함께 반환합니다.
                    """
    )
    public ApiResponse<List<RecycleScheduleResponse>>
    getSchedulesByApartment(

            @RequestParam
            Long apartmentId
    ) {
        List<RecycleScheduleResponse> response =
                recycleScheduleQueryService
                        .getSchedulesByApartment(
                                apartmentId
                        );

        return ApiResponse.success(
                "아파트별 배출 일정 목록 조회 성공",
                response
        );
    }

    /**
     * 공식 배출 일정 한 건을 상세 조회합니다.
     */
    @GetMapping("/{scheduleId}")
    @Operation(
            summary = "관리자 배출 일정 상세 조회",
            description = """
                    관리자가 배출 일정 ID를 기준으로
                    공식 배출 일정 한 건을 조회합니다.
                    """
    )
    public ApiResponse<RecycleScheduleResponse>
    getSchedule(

            @PathVariable
            Long scheduleId
    ) {
        RecycleScheduleResponse response =
                recycleScheduleQueryService
                        .getSchedule(scheduleId);

        return ApiResponse.success(
                "배출 일정 상세 조회 성공",
                response
        );
    }

    /**
     * 새로운 공식 배출 일정을 등록합니다.
     */
    @PostMapping
    @Operation(
            summary = "공식 배출 일정 등록",
            description = """
                    관리자가 승인된 아파트와 폐기물 품목에
                    공식 배출 일정을 등록합니다.

                    상시 배출 일정이 아니라면
                    요일, 시작 시간, 종료 시간이 필요합니다.

                    같은 아파트와 품목에 상시 일정과
                    요일별 일정을 동시에 등록할 수 없습니다.
                    """
    )
    public ApiResponse<RecycleScheduleResponse>
    createSchedule(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @Valid
            @RequestBody
            CreateRecycleScheduleRequest request
    ) {
        RecycleScheduleResponse response =
                recycleScheduleAdminService
                        .createSchedule(
                                userDetails.getUserId(),
                                request
                        );

        return ApiResponse.success(
                "공식 배출 일정이 등록되었습니다.",
                response
        );
    }

    /**
     * 공식 배출 일정의 요일과 시간을 수정합니다.
     */
    @PatchMapping("/{scheduleId}")
    @Operation(
            summary = "공식 배출 일정 수정",
            description = """
                    관리자가 기존 공식 배출 일정의
                    요일, 시작 시간, 종료 시간 또는
                    상시 배출 여부를 수정합니다.

                    일정의 아파트와 폐기물 품목은
                    변경되지 않습니다.
                    """
    )
    public ApiResponse<RecycleScheduleResponse>
    updateSchedule(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @PathVariable
            Long scheduleId,

            @Valid
            @RequestBody
            UpdateRecycleScheduleRequest request
    ) {
        RecycleScheduleResponse response =
                recycleScheduleAdminService
                        .updateSchedule(
                                userDetails.getUserId(),
                                scheduleId,
                                request
                        );

        return ApiResponse.success(
                "공식 배출 일정이 수정되었습니다.",
                response
        );
    }

    /**
     * 공식 배출 일정을 삭제합니다.
     */
    @DeleteMapping("/{scheduleId}")
    @Operation(
            summary = "공식 배출 일정 삭제",
            description = """
                    관리자가 등록된 공식 배출 일정을
                    완전히 삭제합니다.
                    """
    )
    public ApiResponse<Void>
    deleteSchedule(

            @AuthenticationPrincipal
            CustomUserDetails userDetails,

            @PathVariable
            Long scheduleId
    ) {
        recycleScheduleAdminService
                .deleteSchedule(
                        userDetails.getUserId(),
                        scheduleId
                );

        return ApiResponse.success(
                "공식 배출 일정이 삭제되었습니다."
        );
    }
}