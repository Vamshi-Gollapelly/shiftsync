package com.shiftsync.staff.dto;

import com.shiftsync.staff.Role;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateStaffRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email,

        @NotBlank(message = "Temporary password is required")
        @Size(min = 10, message = "Password must be at least 10 characters")
        String temporaryPassword,

        @NotNull(message = "Role is required")
        Role role,

        @DecimalMin(value = "0.0", inclusive = true, message = "Hourly rate cannot be negative")
        BigDecimal hourlyRate
) {}