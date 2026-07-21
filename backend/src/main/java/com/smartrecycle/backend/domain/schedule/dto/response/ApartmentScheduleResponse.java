package com.smartrecycle.backend.domain.schedule.dto.response;

import com.smartrecycle.backend.domain.apartment.entity.Apartment;

import java.time.LocalDateTime;
import java.util.List;

public record ApartmentScheduleResponse(

        /**
         * 일정이 적용되는 아파트 ID
         */
        Long apartmentId,

        /**
         * 일정이 적용되는 아파트 이름
         */
        String apartmentName,

        /**
         * 배출 가능 여부 계산에 사용한 기준 일시
         */
        LocalDateTime referenceDateTime,

        /**
         * 아파트에 등록된 품목별 공식 배출 일정
         */
        List<WasteItemScheduleResponse> items

) {

    public static ApartmentScheduleResponse of(
            Apartment apartment,
            LocalDateTime referenceDateTime,
            List<WasteItemScheduleResponse> items
    ) {
        return new ApartmentScheduleResponse(
                apartment.getId(),
                apartment.getName(),
                referenceDateTime,
                List.copyOf(items)
        );
    }
}