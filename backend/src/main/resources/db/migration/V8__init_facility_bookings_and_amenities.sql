-- =========================================================================
-- V8: SCOS Facility Bookings & Amenity Reservation Schema
-- Multi-tenant facility catalog, slot reservations, approval workflows, and audit history.
-- =========================================================================

CREATE TABLE scos_facilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES scos_communities(id) ON DELETE CASCADE,
    facility_code VARCHAR(40) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL
        CHECK (category IN ('CLUBHOUSE', 'SWIMMING_POOL', 'SPORTS_COURT', 'PARTY_HALL', 'GYM', 'GUEST_ROOM', 'COWORKING', 'OTHER')),
    building_id UUID REFERENCES scos_buildings(id) ON DELETE SET NULL,
    location_details VARCHAR(200),
    capacity INT NOT NULL DEFAULT 1 CHECK (capacity > 0),
    hourly_fee NUMERIC(10,2) NOT NULL DEFAULT 0.00 CHECK (hourly_fee >= 0),
    security_deposit NUMERIC(10,2) NOT NULL DEFAULT 0.00 CHECK (security_deposit >= 0),
    currency_code VARCHAR(3) NOT NULL DEFAULT 'INR',
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    operating_hours_start TIME NOT NULL DEFAULT '06:00:00',
    operating_hours_end TIME NOT NULL DEFAULT '22:00:00',
    min_booking_hours INT NOT NULL DEFAULT 1 CHECK (min_booking_hours > 0),
    max_booking_hours INT NOT NULL DEFAULT 6 CHECK (max_booking_hours >= min_booking_hours),
    cancellation_cutoff_hours INT NOT NULL DEFAULT 2 CHECK (cancellation_cutoff_hours >= 0),
    rules_and_regulations TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(60) NOT NULL DEFAULT 'system',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(60) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_scos_facility_community_code UNIQUE (community_id, facility_code),
    CONSTRAINT uq_scos_facility_community_id UNIQUE (community_id, id)
);

CREATE INDEX idx_scos_facilities_community_category
    ON scos_facilities (community_id, category, is_active);
CREATE INDEX idx_scos_facilities_building
    ON scos_facilities (community_id, building_id) WHERE building_id IS NOT NULL;

CREATE TABLE scos_facility_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES scos_communities(id) ON DELETE CASCADE,
    booking_reference VARCHAR(32) NOT NULL,
    facility_id UUID NOT NULL,
    resident_user_id UUID NOT NULL REFERENCES scos_users(id) ON DELETE RESTRICT,
    unit_id UUID REFERENCES scos_units(id) ON DELETE SET NULL,
    booking_date DATE NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    attendees_count INT NOT NULL DEFAULT 1 CHECK (attendees_count > 0),
    purpose VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'CONFIRMED'
        CHECK (status IN ('PENDING_APPROVAL', 'CONFIRMED', 'CHECKED_IN', 'COMPLETED', 'CANCELLED', 'REJECTED')),
    total_fee NUMERIC(10,2) NOT NULL DEFAULT 0.00 CHECK (total_fee >= 0),
    deposit_amount NUMERIC(10,2) NOT NULL DEFAULT 0.00 CHECK (deposit_amount >= 0),
    currency_code VARCHAR(3) NOT NULL DEFAULT 'INR',
    approved_by_user_id UUID REFERENCES scos_users(id) ON DELETE SET NULL,
    approved_at TIMESTAMPTZ,
    rejection_reason VARCHAR(300),
    cancelled_by_user_id UUID REFERENCES scos_users(id) ON DELETE SET NULL,
    cancelled_at TIMESTAMPTZ,
    cancellation_reason VARCHAR(300),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_scos_booking_community_ref UNIQUE (community_id, booking_reference),
    CONSTRAINT fk_scos_booking_facility
        FOREIGN KEY (community_id, facility_id)
        REFERENCES scos_facilities (community_id, id) ON DELETE RESTRICT,
    CONSTRAINT ck_scos_booking_window CHECK (end_time > start_time)
);

CREATE INDEX idx_scos_bookings_community_facility_date
    ON scos_facility_bookings (community_id, facility_id, booking_date, status);
CREATE INDEX idx_scos_bookings_resident
    ON scos_facility_bookings (community_id, resident_user_id, status);
CREATE INDEX idx_scos_bookings_time_window
    ON scos_facility_bookings (community_id, facility_id, start_time, end_time)
    WHERE status IN ('PENDING_APPROVAL', 'CONFIRMED');

CREATE TABLE scos_facility_booking_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES scos_communities(id) ON DELETE CASCADE,
    booking_id UUID NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    reason VARCHAR(300),
    actor_user_id UUID REFERENCES scos_users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_scos_booking_history_order
    ON scos_facility_booking_history (community_id, booking_id, created_at DESC);

-- Screen Permission and RBAC grants
INSERT INTO scos_permissions (id, code, module, description) VALUES
    ('00000000-0000-0000-0001-000000000009', 'SCREEN:FACILITY_BOOKINGS', 'SCREEN', 'Access Amenities & Facility Bookings Desk')
ON CONFLICT (code) DO NOTHING;

-- Grant to SUPER_ADMIN, COMMUNITY_MANAGER, HOA_OFFICER, OWNER, TENANT
INSERT INTO scos_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM scos_roles r CROSS JOIN scos_permissions p
WHERE p.code = 'SCREEN:FACILITY_BOOKINGS'
  AND r.code IN ('SUPER_ADMIN', 'COMMUNITY_MANAGER', 'HOA_OFFICER', 'OWNER', 'TENANT')
ON CONFLICT DO NOTHING;

COMMENT ON TABLE scos_facilities IS 'Community amenities and bookable facilities catalog.';
COMMENT ON TABLE scos_facility_bookings IS 'Resident amenity reservations with conflict prevention and approval workflow.';
COMMENT ON TABLE scos_facility_booking_history IS 'Append-only audit trail for booking status lifecycle changes.';
