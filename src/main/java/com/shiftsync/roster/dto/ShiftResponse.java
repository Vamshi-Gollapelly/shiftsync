package com.shiftsync.roster.dto;

import com.shiftsync.roster.Shift;
import com.shiftsync.roster.ShiftStatus;

import java.time.Instant;
import java.util.UUID;

public record ShiftResponse(
        UUID id,
        UUID staffId,
        Instant startTime,
        Instant endTime,
        boolean publicHoliday,
        String penaltyRateReason,
        ShiftStatus status,
        Instant createdAt
) {
    public static ShiftResponse from(Shift shift) {
        return new ShiftResponse(
                shift.getId(),
                shift.getStaffId(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.isPublicHoliday(),
                shift.getPenaltyRateReason(),
                shift.getStatus(),
                shift.getCreatedAt()
        );
    }
}