-- V7: Work-order attachment metadata.
-- Depends on the canonical SCOS work-order schema from V5 and asset extensions from V6.
-- File bytes live in private object storage; PostgreSQL stores metadata only.
-- Keep migration history additive and do not recreate V5 work-order domain tables.

ALTER TABLE scos_work_orders
    ADD CONSTRAINT uq_scos_work_orders_community_id UNIQUE (community_id, id);

CREATE TABLE scos_work_order_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_id UUID NOT NULL REFERENCES scos_communities(id) ON DELETE RESTRICT,
    work_order_id UUID NOT NULL,
    object_key VARCHAR(700) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    byte_size BIGINT NOT NULL CHECK (byte_size > 0),
    sha256_hex CHAR(64),
    uploaded_by_user_id UUID NOT NULL REFERENCES scos_users(id) ON DELETE RESTRICT,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    caption VARCHAR(500),
    is_resident_visible BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_scos_wo_attachment_object UNIQUE (community_id, object_key),
    CONSTRAINT fk_scos_wo_attachment_work_order
        FOREIGN KEY (community_id, work_order_id)
        REFERENCES scos_work_orders (community_id, id) ON DELETE RESTRICT
);

CREATE INDEX idx_scos_wo_attachments_order
    ON scos_work_order_attachments (community_id, work_order_id, uploaded_at DESC);

COMMENT ON TABLE scos_work_order_attachments IS
    'Private object-storage metadata only; file bytes are not stored in PostgreSQL.';
