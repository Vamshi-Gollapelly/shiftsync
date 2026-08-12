package com.shiftsync.staff.dto;

import com.shiftsync.staff.AppUser;
import com.shiftsync.staff.Role;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StaffResponse(
        UUID id,
        String fullName,
        String email,
        Role role,
        BigDecimal hourlyRate,
        boolean active,
        Instant createdAt
) {
    public static StaffResponse from(AppUser user) {
        return new StaffResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getHourlyRate(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}