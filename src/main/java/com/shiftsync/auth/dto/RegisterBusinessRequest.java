package com.shiftsync.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterBusinessRequest(
        @NotBlank(message = "Business name is required")
        String businessName,

        @NotBlank(message = "Slug is required")
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "Slug must be lowercase letters, numbers, and hyphens only (e.g. 'cafe-lulu')")
        String slug,

        @NotBlank(message = "Owner name is required")
        String ownerFullName,

        @NotBlank(message = "Owner email is required")
        @Email(message = "Owner email must be a valid email address")
        String ownerEmail,

        @NotBlank(message = "Password is required")
        @Size(min = 10, message = "Password must be at least 10 characters")
        String password
) {}