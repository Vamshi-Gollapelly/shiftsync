-- ShiftSync initial schema
-- Multi-tenant design: every business-owned table carries an explicit business_id
-- column and every query in the app is required to filter on it at the service
-- layer (see TenantContext + service classes). This is deliberate: an implicit
-- global filter is easy to forget to apply to a *new* query later; an explicit
-- business_id parameter on every repository method makes tenant leakage a
-- compile-time-visible mistake instead of a silent runtime one.

-- gen_random_uuid() needs pgcrypto enabled before it's used in DEFAULT clauses below
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE businesses (
                            id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Email is only unique WITHIN a business (see app_users below), which
    -- means email alone can't identify which tenant to log into. `slug` is
    -- the public, human-readable tenant identifier a user provides at login
    -- (e.g. "cafe-lulu") — the same pattern real multi-tenant SaaS products
    -- use as a subdomain (cafe-lulu.shiftsync.com).
                            slug            VARCHAR(80) NOT NULL UNIQUE,
                            name            VARCHAR(255) NOT NULL,
                            abn             VARCHAR(20),
                            timezone        VARCHAR(64) NOT NULL DEFAULT 'Australia/Melbourne',
                            created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE app_users (
                           id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           business_id     UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
                           email           VARCHAR(255) NOT NULL,
                           password_hash   VARCHAR(255) NOT NULL,
                           full_name       VARCHAR(255) NOT NULL,
                           role            VARCHAR(20)  NOT NULL CHECK (role IN ('OWNER', 'MANAGER', 'STAFF')),
                           hourly_rate     NUMERIC(8,2),
                           active          BOOLEAN NOT NULL DEFAULT true,
                           created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- email only needs to be unique WITHIN a business, not globally: two
    -- different cafes can both have an owner using the same email address.
                           CONSTRAINT uq_user_email_per_business UNIQUE (business_id, email)
);

CREATE INDEX idx_app_users_business_id ON app_users(business_id);

CREATE TABLE shifts (
                        id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        business_id         UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
                        staff_id            UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
                        start_time          TIMESTAMPTZ NOT NULL,
                        end_time            TIMESTAMPTZ NOT NULL,
                        is_public_holiday   BOOLEAN NOT NULL DEFAULT false,
                        penalty_rate_reason VARCHAR(50),
                        status              VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
                            CHECK (status IN ('SCHEDULED', 'CANCELLED', 'COMPLETED')),
                        created_by          UUID NOT NULL REFERENCES app_users(id),
                        created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
                        CONSTRAINT chk_shift_time_order CHECK (end_time > start_time)
);

CREATE INDEX idx_shifts_business_id ON shifts(business_id);
CREATE INDEX idx_shifts_staff_id ON shifts(staff_id);
-- speeds up the overlap-conflict check, which is the hottest query in the app
CREATE INDEX idx_shifts_staff_time_range ON shifts(staff_id, start_time, end_time);

CREATE TABLE audit_logs (
                            id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            business_id     UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
                            actor_user_id   UUID REFERENCES app_users(id),
                            action          VARCHAR(100) NOT NULL,
                            entity_type     VARCHAR(50) NOT NULL,
                            entity_id       UUID,
                            details         JSONB,
                            created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_business_id ON audit_logs(business_id);