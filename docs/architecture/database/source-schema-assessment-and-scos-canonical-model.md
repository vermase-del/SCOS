# SCOS Canonical Database Design — Assessment of DBSchema.json

**Status:** Proposed canonicalization baseline  
**Source inspected:** `DBSchema.json` — SQL Server-style schema inventory  
**Target platform:** SCOS, PostgreSQL 16+, Flyway, Java 21 / Spring Boot, UUID identifiers, Angular  
**Decision:** Reuse domain patterns and workflows, not the source database's physical schema verbatim.

## 1. Executive assessment

The supplied schema inventory contains **1,347 tables and approximately 25,080 columns**. It resembles a mature hospitality/property-management platform spanning property inventory, guest profiles, reservations/stays, receivables, invoicing, payments, leases, room access, housekeeping, work orders, enterprise maintenance, messaging, audit, reporting, data warehouse, interface queues, and legacy compatibility objects.

It is a valuable reference for SCOS because it demonstrates mature operational patterns: a property boundary, unit/room status history, resident/guest profiles separated from stays, itemized charges, invoices and payments, lease billing schedules, maintenance work orders, housekeeping/inspection records, access control, and activity history.

It is **not** a good idea to copy all 1,347 tables into SCOS or rename every column mechanically. The source mixes several product generations and subsystems (including `P5*`, `R5*`, `H5*`, `DW_*`, staging, interface, reporting and archive tables). Many columns are nullable, use short code fields, repeat generic UDF columns, and store several concepts in the same record. The supplied inventory exposes table/column metadata but does not provide foreign-key constraints, indexes, check constraints, triggers, or actual data distributions. Relationship assumptions must therefore be validated before implementation.

## 2. Canonical SCOS database conventions

Use these rules for new SCOS tables and future refactoring:

- **Table names:** `scos_<plural_snake_case>`, e.g. `scos_work_orders`, `scos_resident_profiles`.
- **Column names:** descriptive `snake_case`, e.g. `community_id`, `unit_id`, `planned_start_at`.
- **Primary keys:** UUID `id` with `gen_random_uuid()` unless a stable natural key or composite key is a genuine domain requirement.
- **Foreign keys:** explicit PostgreSQL foreign keys; add indexes on FK columns used for joins. Prefer `RESTRICT` for financial/history references and `CASCADE` only for dependent detail rows whose lifecycle truly belongs to the parent.
- **Community isolation:** include `community_id` on community-owned aggregate/root records. Use composite constraints such as `UNIQUE (community_id, code)`; where cross-community references are possible, enforce ownership through composite foreign keys or validated service constraints—not only a UI filter.
- **Time:** `TIMESTAMPTZ` for events and audit times; `DATE` for calendar dates. Store timestamps consistently in UTC and render in the community's timezone.
- **Money:** `NUMERIC(14,2)` or an explicitly documented precision; never floating point. Store `currency_code` when values can be multi-currency. Keep immutable transaction/ledger rows instead of relying on a mutable balance.
- **Quantities and measurements:** explicit units in names or reference tables, e.g. `area_sqft`, `quantity`, `unit_price`.
- **Lifecycle/audit:** `created_at`, `created_by`, `updated_at`, `updated_by`; use optimistic `version` where concurrent editing is expected. Operational history and security audit logs remain separate.
- **Statuses/categories:** check constraints for small stable enums; reference/configuration tables when administrators need to configure values without code deployment.
- **No generic UDF columns as the default:** replace source-style `UDFCHAR01`, `UDFNUM01`, etc. with typed extension tables or JSONB only for truly optional, governed metadata.
- **Avoid storing derived values twice** unless a deliberate performance/cache strategy exists. If totals are cached, reconcile them against ledger lines.
- **PII minimization:** collect only the identity/contact data required for apartment operations. Do not import passport/visa/ID-document fields or hospitality-specific guest profiling into the resident model by default.

## 3. Source-to-SCOS domain mapping

| Source tables/patterns | Reusable concept | SCOS canonical destination / recommendation |
|---|---|---|
| `Property`, `PropertyAttribute`, `P5PROPERTYBUILDING` | Property/community identity, address, timezone, configuration, building metadata | Keep `scos_communities` as tenant root; extend with community contact/address/timezone fields only where absent. Use `scos_buildings` for towers/blocks and a typed `scos_community_settings` or `scos_community_attributes` for genuinely configurable settings. Do not create a second property root. |
| `Room`, `RoomType`, `RoomStatus`, `P5ROOMSTATUS` | Unit inventory, classification, occupancy and time-based status history | Retain `scos_units` and `scos_buildings`; add `scos_unit_status_history` for effective-dated status changes. Map room → unit, room type → unit type, floor/wing/building → explicit unit/building attributes. |
| `GuestNameInfo`, `GuestProfileRevenue`, `DW_GuestProfile` | Person/profile separated from a reservation or stay | Reuse `scos_users` for authentication identity. Introduce `scos_resident_profiles` for resident-specific profile/contact details only if the current user entity is intentionally minimal; use `scos_unit_occupancies` / existing `scos_unit_occupants` for time-bounded ownership, tenancy, family/dependent, and emergency-contact relationships. Do not import hospitality guest IDs and passport fields. |
| `Reservation`, `ReservationStay`, `ReservationStayDate`, `ReservationActivity`, `ReservationComment` | Time-bounded occupancy/booking, status transitions, date detail, comments | Map long-term residential occupancy to `scos_leases` and `scos_lease_terms`; short-stay/guest facilities booking to a separate `scos_facility_bookings` domain. Keep booking separate from unit ownership/occupancy and from resident identity. |
| `P5LEASECONFIG`, `P5LEASEBILLING`, `P5LEASESCHEDULE`, `P5SECURITYDEPOSITLEDGERDETAIL` | Lease contract, scheduled rent/maintenance, invoice period, deposits and deposit movements | Add `scos_leases`, `scos_lease_charges` or `scos_lease_charge_schedules`, `scos_security_deposits`, and append-only `scos_security_deposit_transactions`. Preserve effective dates, due dates, billing status, and source/reference IDs. |
| `Account`, `AR_Account`, `AR_AccountCharge`, `AR_Charge`, `AR_ChargeDetail`, `P5FOLIOCHARGE` | Customer account, receivable account, charge header/detail, itemized folio ledger | Build a clean finance domain: `scos_financial_accounts`, `scos_charges`, `scos_charge_lines`, and append-only `scos_ledger_transactions`. Work-order costs are operational cost evidence, not automatically resident charges. Add a separate charge/approval/posting flow before billing a resident. |
| `AR_Invoice`, `AR_InvoiceItem`, `AR_Statement`, `AR_StatementItem` | Invoice header/lines and resident statements | `scos_invoices`, `scos_invoice_lines`, `scos_account_statements`, `scos_account_statement_lines`. Freeze issued invoice snapshots; corrections should use credit notes/reversal entries, not mutate issued totals. |
| `AR_Payment`, `AR_PaymentDetail`, `AR_PaymentInvoice`, `AR_PaymentBatch*` | Payment receipt, allocation, batch/reconciliation | `scos_payments`, `scos_payment_allocations`, `scos_payment_batches`, `scos_payment_reconciliation_items`. Keep payment provider references/idempotency keys; never store raw card data. |
| `P5WORKORDERS` | Maintenance request/work order, room/unit, status/reason, urgency, planned/actual dates, assigned person, department, out-of-service status, notes | Evolve `scos_work_orders` to include typed `work_order_type`, `reason_code`, `department_id`, `planned_start_at`, `planned_end_at`, `actual_start_at`, `resolved_at`, `out_of_service`, `service_type`, and optional `source_reference`. Keep current ticket, SLA, reporter, assignee, and community scope. Use `scos_work_order_activities` and `scos_work_order_assignments` for history rather than overwriting it. |
| `R5CUSTOMERREQUESTHISTORY`, `R5REQCLASSES`, `R5REQUIRCODES` | Field-change history, request classes and reason codes | Use immutable `scos_work_order_activities` for user-visible lifecycle events, `scos_audit_logs` for security/admin changes, and configurable `scos_work_order_categories` / `scos_work_order_reason_codes` only if community-configurable taxonomy is required. |
| `P5ROOMCLEANING`, `P5HOUSEKEEPINGHOMESECTION` | Cleaning assignment, schedule, duration, inspection, failed inspection | `scos_housekeeping_tasks`, `scos_housekeeping_task_assignments`, `scos_housekeeping_inspections`. Do not overload work orders for routine cleaning checklists; create a work order when a defect or repair is found. |
| `P5ROOMACCESS`, `P5ROOMKEY`, `P5ROOMKEYTRACKDATA`, `P5ROOMACCESS` | Area access configuration, key issue/return, access history | `scos_access_areas`, `scos_access_credentials`, `scos_access_grants`, `scos_access_events`. Keep secret credential material outside general tables and record revocation/audit history. |
| `R5USERS`, `R5ROLES`, `EmployeeRole`, `EmployeeNameRole`, `EmployeePermission` | Legacy staff accounts and role/permission matrix | Keep SCOS `scos_users`, community membership/access and existing RBAC as the source of truth. Map employee/technician to user + profile/assignment, not a second login table. Add explicit community-scoped role assignments only if current membership model cannot express them. |
| `Messages`, `MessageType`, `MessagePullQueue`, `EmailNotifierLog`, `AdaptorMessageLog` | Message templates, delivery queues, retry/error logs | `scos_notification_templates`, `scos_notification_events`, `scos_notification_deliveries`, `scos_notification_attempts`. Separate business events from channel-specific delivery and make delivery idempotent/retryable. |
| `H5AUDITHISTORY`, `R5AUDITPK`, `R5DETAILAUDIT`, `UserAccessLog` | Audit trail and access history | Retain `scos_audit_logs` for attributable security/admin actions and optionally `scos_access_logs` for sign-in/access events. Keep business activity timelines in each domain. |
| `FileResource`, `FileResourceLink`, `P5DOCUMENTLINK`, `P5SCANNEDDOCUMENT*` | Reusable file/document linkage | `scos_files` for object-store metadata and `scos_entity_attachments` for links to a work order, invoice, visitor, incident, or other domain entity. Store bytes in object storage (e.g. MinIO), not PostgreSQL large text columns. |
| `CodeType`, `CodeValue`, `CodeValuePropMap` | Configurable codes and labels | Use domain-specific lookup tables for values with behavior/relationships. Use `scos_reference_codes` only for truly generic, low-complexity lists; avoid one universal code table for unrelated domain logic. |
| `DW_*`, `ODS*`, `t1*`, `t2*`, `RmsStaging*`, `*_Restrict`, `*_ARCHIVE`, `*_TEMP`, `_Merci*` | Data warehouse, staging, legacy restriction/archive and vendor integration internals | Do not copy into the SCOS transactional schema. Add a separate analytics/read model, import staging schema, integration outbox/inbox, and retention/archive policy only when an actual SCOS requirement calls for them. |

## 4. Recommended SCOS domain boundaries

Use the following as a target catalogue. This is a roadmap, not a recommendation to create every table in one migration.

### A. Community and property foundation
- Existing: `scos_communities`, `scos_buildings`, `scos_units`, `scos_unit_occupants`.
- Add when needed: `scos_community_settings`, `scos_unit_status_history`, `scos_parking_slots`, `scos_common_areas`, `scos_assets`, `scos_asset_status_history`.
- Keep a single canonical community root and ensure each building/unit belongs to the same community as its parent.

### B. Identity, people, and occupancy
- Existing: `scos_users` and the existing community access/RBAC model.
- Add when needed: `scos_resident_profiles`, `scos_leases`, `scos_lease_occupants`, `scos_guest_visits`, `scos_emergency_contacts`.
- Separate authentication identity from resident/person attributes and from time-bounded legal occupancy.

### C. Facilities and reservations
- Add: `scos_facilities`, `scos_facility_bookings`, `scos_facility_booking_participants`, `scos_booking_status_history`.
- Enforce no overlapping active bookings with PostgreSQL exclusion constraints where booking intervals and resources allow it.

### D. Maintenance and asset operations
- Existing on feature branch: `scos_work_orders`, `scos_work_order_sequences`, `scos_work_order_activities`, `scos_work_order_assignments`, `scos_work_order_cost_lines`, `scos_work_order_feedback`, `scos_work_order_sla_history`.
- Evolve work-order columns based on `P5WORKORDERS` patterns; add separate `scos_assets`, `scos_asset_maintenance_plans`, `scos_maintenance_plan_tasks`, `scos_work_order_materials`, `scos_vendors`, and `scos_work_order_attachments` when workflows require them.
- Separate planned preventive maintenance from reactive complaints. Both can generate work orders but should not be one overloaded record.

### E. Finance and billing
- Add in phases: `scos_financial_accounts`, `scos_charge_codes`, `scos_charges`, `scos_charge_lines`, `scos_invoices`, `scos_invoice_lines`, `scos_payments`, `scos_payment_allocations`, `scos_credit_notes`, `scos_security_deposits`, `scos_security_deposit_transactions`.
- Keep maintenance estimate/actual cost, association expense, resident charge, invoice, payment and ledger posting as distinct lifecycle stages.

### F. Visitor, access, and security
- Add in phases: `scos_visitors`, `scos_visit_requests`, `scos_visitor_checkpoints`, `scos_access_areas`, `scos_access_grants`, `scos_access_events`, `scos_incidents`.
- Use auditable state transitions, expiry/revocation, and least-privilege access.

### G. Communications, files, and audit
- Add in phases: `scos_notification_templates`, `scos_notification_events`, `scos_notification_deliveries`, `scos_files`, `scos_entity_attachments`.
- Keep `scos_audit_logs` separate from domain activity tables; use outbox/inbox patterns for reliable integrations.

### H. Analytics
- Build views/materialized views or a separate analytics schema over verified transactional data. Do not import the source's entire data warehouse structure before SCOS KPIs and retention requirements are defined.

## 5. Work Orders Desk changes suggested by the source schema

The current SCOS V5 schema is a good clean starting point and should not be replaced by the 60+ columns in `P5WORKORDERS`. It should be extended with explicit apartment-domain fields where useful:

| Suggested SCOS column | Source pattern | Purpose |
|---|---|---|
| `work_order_type` | Work-order / service-type fields | Reactive complaint vs preventive maintenance vs inspection |
| `reason_code` | `WOS_REASON`, `RQM_CODE` | Structured reason/category separate from free text |
| `department_id` or `department_code` | `WOS_DEPARTMENT` | Assign plumbing/electrical/security/housekeeping to a managed team |
| `planned_start_at`, `planned_end_at` | `WOS_STARTDATE`, `WOS_PLANNEDENDDATE` | Scheduling and SLA/work planning |
| `actual_start_at`, `resolved_at` | `WOS_STARTDATE`, `WOS_BACKINSERVICEDATE` | Actual work and return-to-service timestamps |
| `out_of_service` | `WOS_OUTOFORDER` | Mark an affected asset/amenity unavailable |
| `service_type` | `WOS_SERVICETYPE` | Preventive/inspection/repair classification when taxonomy differs from ticket category |
| `asset_id` | Room/equipment references | Link work orders to elevators, pumps, generators, CCTV, fire systems, and other common assets |
| `external_reference` | PMS confirmation / source references | Safe traceability to integrations without copying vendor keys into primary identifiers |

Add these through a **new Flyway migration** after the current V5 migration; do not edit already-released migration history. Keep JPA entities, DTOs, APIs, and UI synchronized in the same PR. Add migration tests against a clean PostgreSQL database before considering it ready to merge.

## 6. Important structural improvements over the source

1. **Strong tenancy:** community ownership should be enforceable in the database where feasible, not only by service checks.
2. **Effective-dated relationships:** resident/unit occupancy and unit status require start/end dates and a rule for overlapping active periods.
3. **History rather than overwrite:** status transitions, assignment changes, deposit transactions, payments, and notification attempts should be append-only or versioned.
4. **Separation of concerns:** operational work order cost is not a bill; an invoice is not a payment; a visitor request is not an access grant; a person is not a lease.
5. **Data protection:** the source guest table includes passport, visa, identity and family fields that SCOS should not replicate unless there is a clearly approved legal need.
6. **No opaque abbreviations:** replace names such as `WOS_*`, `FOC_*`, `LCB_*` with explicit domain names.
7. **Avoid premature over-modeling:** do not build every source module in advance. Add modules in product roadmap order and migrate only the fields and lifecycle rules actually required.
8. **Constraints and indexes:** source metadata does not show FKs/indexes/check constraints. Define these intentionally in SCOS and verify them through migration tests.

## 7. Implementation order

1. Treat this document as the source-to-SCOS mapping baseline.
2. Validate current SCOS entities and migrations against the canonical naming and tenancy rules.
3. Apply Work Orders additions via a new migration and keep service/UI/API aligned.
4. Add resident/lease/guest and facility-booking models when their product workflows are scheduled.
5. Implement finance in ledger-first stages: charges → invoices → payments/allocations → reconciliation.
6. Add asset preventive maintenance, housekeeping, access, visitor and notification domains as distinct bounded contexts.
7. Generate an ER diagram and data dictionary from the actual SCOS PostgreSQL migrations; do not infer the final relationship graph from this source inventory alone.
8. Require clean-database migration tests, FK/tenant-isolation tests, and lifecycle integration tests before merging.

## 8. Scope decision

**Adopt:** useful business concepts and proven lifecycle patterns from the uploaded schema.

**Do not adopt:** vendor table names, legacy abbreviations, all 1,347 tables, hospitality-only reservation/rate/channel logic, generic UDF columns, denormalized warehouse/staging objects, or passport-heavy guest records by default.

The target is a coherent SCOS-native PostgreSQL schema that fits apartment communities and can evolve safely—not a renamed clone of a hospitality PMS.
