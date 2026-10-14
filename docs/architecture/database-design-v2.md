# SCOS Database Design — Domain-First PostgreSQL Model

**Status:** Proposed implementation baseline  
**Database:** PostgreSQL 16+  
**Migration strategy:** Additive Flyway migrations; never edit a migration that may already have been applied  
**Reference input:** Supplied `DBSchema.json` schema reference (used to identify business concepts and relationships, not copied table-for-table)

## 1. Design decision

SCOS should use a normalized, domain-first schema rather than reproduce the source schema's 1,300+ tables. The supplied schema is useful as a business vocabulary/reference for property, resident, work management, billing, cash, vendors, and audit concepts. SCOS table and column names should be descriptive, PostgreSQL-native, tenant-scoped, and owned by clear application modules.

Existing foundation tables remain authoritative:
- `scos_communities`: tenant/community boundary
- `scos_buildings` and `scos_units`: physical property inventory
- `scos_users`, `scos_roles`, `scos_permissions`, `scos_user_community_access`: identity and access control

Do not create duplicate community, building, unit, or user master tables inside feature modules.

## 2. Modeling rules

1. **Tenant isolation:** every community-owned business record has a non-null `community_id`; queries must always be scoped by the authenticated community. Add PostgreSQL row-level security only after the service reliably sets a transaction-local tenant context.
2. **Keys:** use UUID primary keys. Keep human-facing identifiers (ticket number, invoice number, visitor pass number) as separate unique business keys.
3. **Relationships:** use explicit foreign keys and uniqueness constraints. Add composite tenant-aware foreign keys where the parent schema supports them; otherwise enforce tenant membership in service authorization and add integration tests.
4. **Lifecycle/history:** don't overwrite meaningful history. Use status transitions, assignment history, immutable activity events, invoice/payment allocations, and effective-dated ownership/occupancy records.
5. **Money:** use `NUMERIC(14,2)` and an explicit ISO currency code; never use floating point for money. Use `NUMERIC(12,3)` for measured quantities.
6. **Time:** use `TIMESTAMPTZ` for instants, `DATE` for effective dates, and store the community's IANA time zone in community settings. Persist calculated SLA deadlines in UTC.
7. **Audit:** operational timeline events are not a replacement for platform security audit logs. Keep business activities separate from `scos_audit_logs`.
8. **Deletion:** financial and operational records should generally be cancelled/archived, not hard-deleted. Use `ON DELETE RESTRICT` for business history and `CASCADE` only for true dependent child rows.
9. **Flexible data:** use JSONB only for optional provider payloads, configurable metadata, or snapshots—not as a substitute for relational columns and foreign keys.
10. **Migrations:** V1–V4 remain immutable. Add schema changes through new, ordered Flyway migrations and test upgrades from both an empty database and the previous released schema.

## 3. Domain model

### A. Community and property

Reuse the existing community/building/unit masters. Add the following when the respective module is implemented:

| Table | Purpose | Important relationships / rules |
|---|---|---|
| `scos_unit_ownerships` | Ownership history and ownership share | Community + unit + owner user; effective start/end dates; share between 0 and 100; retain prior owners |
| `scos_unit_occupancies` | Who occupies a unit and in what capacity | Community + unit + resident; occupancy type OWNER/TENANT/FAMILY; effective dates; emergency/contact flags |
| `scos_leases` | Rental agreement lifecycle | Unit, owner/landlord, primary tenant; lease dates, rent, deposit, currency, renewal and termination data |
| `scos_resident_profiles` | Resident-specific profile details | One-to-one with user; only collect fields needed for service; keep sensitive identity documents in private object storage |
| `scos_household_members` | Household links where the person is not an independent login | Unit/occupancy relationship, relationship type, emergency-contact flag; avoid duplicate copies of user identity data |

Ownership and occupancy must be separate: a landlord can own a unit while a tenant occupies it. A unit's current display status should be derived from active occupancy/maintenance state where practical, not treated as the only source of truth.

### B. Maintenance and complaints (first implementation slice)

Implemented by the canonical V5 and V6 migrations:
- `scos_work_order_sequences`: atomic per-community ticket numbering.
- `scos_work_orders`: current ticket state, location, priority, reporter, optional current assignee, persisted SLA deadlines, hold state, and optimistic-lock version.
- `scos_work_order_activities`: append-only business timeline.
- `scos_work_order_assignments`: assignment/reassignment history.
- `scos_work_order_cost_lines`: itemized labor, parts, material, and contractor cost.
- `scos_work_order_sla_history`: pause/resume/breach timeline.
- `scos_work_order_feedback`: resident rating/comment after resolution.

Use server-side state transitions. The UI must not be allowed to set arbitrary statuses. Keep activity history append-only at the service/database-permission layer. Work-order IDs and all related reads/writes must be authorized against the active community.

### C. Visitor and gate operations

| Table | Purpose | Important rules |
|---|---|---|
| `scos_visitor_passes` | Pre-authorized visitor/vendor/delivery pass | Community, host user/unit, visitor name, visit window, purpose, pass token hash, status, expiry |
| `scos_visitor_events` | Gate check-in/check-out events | Append-only event history, gate/operator, event time, pass/visitor reference |
| `scos_vehicle_registrations` | Resident vehicle registry | Community + unit/occupant, normalized registration number, active flag |

Do not store raw QR/pass secrets; store a cryptographic hash. Visitor event records are historical evidence and should not be overwritten when a visitor checks out.

### D. Finance and accounting

| Table | Purpose | Important rules |
|---|---|---|
| `scos_charge_definitions` | Configurable maintenance/rent/service charges | Effective-dated configuration, taxable flag, default amount and cadence |
| `scos_invoices` | Issued bill/credit note header | Community, unit/account, unique invoice number, issue/due dates, currency, status, subtotal/tax/total/balance snapshot |
| `scos_invoice_lines` | Itemized invoice charges | Invoice, charge code/description snapshot, quantity, unit price, tax rate and amount |
| `scos_payments` | Payment receipt/provider transaction | Unique idempotency/provider reference, amount, currency, method, state, received/refunded timestamps |
| `scos_payment_allocations` | Allocates one payment to one or more invoices | Payment + invoice + allocated amount; prevents over-allocation in a transactional service |
| `scos_journal_entries` / `scos_journal_lines` | Double-entry general ledger | Posted entries immutable; debits must equal credits per currency; reversals instead of edits |
| `scos_vendors` | Approved service providers | Community/global vendor scope, verification status, contacts, tax identifiers protected by role |
| `scos_purchase_orders` | Approved vendor spend | Vendor, approval state, currency, line items, linked work order where applicable |

Never equate an invoice with a payment. An invoice can have multiple payments and a payment can be allocated across multiple invoices. Treat gateway callbacks as idempotent and reconcile settlements asynchronously. Ledger and payment posting should be transactional and auditable.

### E. Community services

| Table | Purpose | Important rules |
|---|---|---|
| `scos_amenities` | Clubhouse, court, guest room, etc. | Community, capacity, opening hours, booking rules, deposit/fee |
| `scos_amenity_bookings` | Resident reservations | Unit/user, time window, status, cancellation reason, fee snapshot; prevent overlapping confirmed bookings |
| `scos_announcements` | Community notices | Author, audience scope, publication window, lifecycle |
| `scos_announcement_reads` | Read receipts where needed | Unique announcement + user |
| `scos_documents` | Metadata for policies, agreements, invoices, evidence | Object key, media type, checksum, access classification, uploader; bytes remain in MinIO/S3 |
| `scos_notifications` | In-app/email/push delivery state | Recipient, template, channel, status, attempt count, provider reference |
| `scos_notification_attempts` | Delivery retry history | Append-only attempt result and timestamps |

Messaging, notifications, and workflow events should not be embedded as JSON arrays on user/community rows. Use separate records with retention policies and authorization checks.

## 4. Critical constraints and indexes

- Unique: `(community_id, ticket_number)`, `(community_id, invoice_number)`, normalized vehicle registration per active community, provider idempotency key.
- Index foreign keys used for joins; PostgreSQL does not automatically index the referencing side of every foreign key.
- Common work-order indexes: `(community_id, status, priority, created_at DESC)`, `(community_id, assigned_to_user_id, status)`, `(community_id, unit_id, created_at DESC)`.
- Common finance indexes: `(community_id, status, due_date)`, `(community_id, unit_id, issued_at DESC)`, provider reference unique indexes.
- Use partial unique indexes for one-active-record rules, for example one active lease per unit where business rules require it.
- Use exclusion constraints (GiST) for non-overlapping active amenity bookings; validate availability again inside the booking transaction.
- Use check constraints for positive quantities, valid date ranges, status enums, percentage ranges, and positive allocation amounts.

## 5. Security and privacy

- Derive `community_id` from authenticated membership, not a trusted client-supplied field alone.
- Verify every referenced user is authorized in the same community before assigning or exposing records.
- Keep identity numbers, bank details, and uploaded documents out of general-purpose JSONB and application logs.
- Use private MinIO/S3 buckets, signed short-lived download URLs, content-type/size checks, malware scanning, and access logging.
- Define retention and deletion policies for visitor logs, messages, documents, and resident profiles.
- Use Flyway service-account permissions and a separate least-privilege runtime database role; application users must not own schema objects.

## 6. Migration plan

1. **V5 — Work Orders Desk:** canonical work-order schema, state lifecycle, SLA tracking, assignments, costs, feedback, and activity history.
2. **V6 — Asset register and preventive maintenance:** community assets, asset status history, and planned maintenance fields on work orders.
3. **V7 — Work-order attachments:** object-storage metadata with tenant-aware foreign keys; keep file bytes in private object storage.
4. **V8 — Resident and occupancy lifecycle:** profiles, ownership history, occupancy, leases, and household members; backfill only after data-quality checks.
5. **V9 — Gate and visitor management:** passes, append-only gate events, vehicle registry.
6. **V10 — Finance foundation:** charge definitions, invoices/lines, payments/allocations, then double-entry ledger and reconciliation.
7. **V11 — Community services:** amenities/bookings, announcements/read receipts, documents, notifications.
8. For each release: test clean install, upgrade path, tenant-isolation integration tests, query plans, and backup/restore.

The detailed source-schema-to-SCOS mapping should be maintained as a separate mapping workbook/document; don't use source-system physical column names in production code.
