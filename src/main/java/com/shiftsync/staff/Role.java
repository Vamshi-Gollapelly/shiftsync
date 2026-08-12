package com.shiftsync.staff;

/**
 * OWNER   - full control over the business, can manage staff and shifts
 * MANAGER - can manage shifts and view staff, cannot manage billing/ownership
 * STAFF   - can view their own shifts only
 *
 * Kept as a simple 3-tier hierarchy on purpose: real hospitality/retail
 * businesses don't need more granularity than this, and over-modelling RBAC
 * for a portfolio project is exactly the kind of unnecessary complexity to
 * avoid.
 */
public enum Role {
    OWNER,
    MANAGER,
    STAFF
}