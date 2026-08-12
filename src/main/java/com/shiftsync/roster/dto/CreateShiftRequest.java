package com.shiftsync.roster.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateShiftRequest(
        @NotNull(message = "Staff ID is required")
        UUID staffId,

        @NotNull(message = "Start time is required")
        @Future(message = "Shift start time must be in the future")
        Instant startTime,

        @NotNull(message = "End time is required")
        @Future(message = "Shift end time must be in the future")
        Instant endTime
) {}