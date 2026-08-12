package com.shiftsync.tenant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A Business is a tenant. Every business-owned row in the system (users,
 * shifts, audit logs) carries a business_id foreign key back to this table,
 * and every service method that touches those tables takes an explicit
 * businessId parameter rather than relying on an implicit global filter.
 */
@Entity
@Table(name = "businesses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business {

    @Id
    @GeneratedValue
    private UUID id;

    /** Public tenant identifier a user provides at login (e.g. "cafe-lulu"). */
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    private String abn;

    @Column(nullable = false)
    @Builder.Default
    private String timezone = "Australia/Melbourne";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}