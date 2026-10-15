package com.scos.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Release Gate Verification Test for PostgreSQL 16+ Flyway Migrations (V1 through V7).
 *
 * Release Gates:
 * 1. Clean Database: Runs all migrations (V1 -> V7) against a fresh PostgreSQL schema.
 * 2. Upgrade Path: Migrates a database at V4 baseline sequentially through V7.
 * 3. Security & Multi-Tenant Isolation: Verifies composite foreign keys enforce community isolation
 *    on scos_work_order_attachments and prevents cross-tenant linkage.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostgresFlywayMigrationReleaseGateTest {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean postgresAvailable = false;

    @BeforeAll
    static void checkPostgresAvailability() {
        dbUrl = System.getenv().getOrDefault("SPRING_DATASOURCE_URL",
                System.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/scos_db_test"));
        dbUser = System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME",
                System.getProperty("spring.datasource.username", "scos_test_user"));
        dbPassword = System.getenv().getOrDefault("SPRING_DATASOURCE_PASSWORD",
                System.getProperty("spring.datasource.password", "scos_test_password"));

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            postgresAvailable = true;
            System.out.println(">>> [POSTGRES RELEASE GATE] Connected successfully to PostgreSQL at " + dbUrl);
        } catch (SQLException e) {
            System.out.println(">>> [POSTGRES RELEASE GATE] PostgreSQL is not available (" + e.getMessage() + "). Skipping integration gates.");
            postgresAvailable = false;
        }
    }

    @BeforeEach
    void requirePostgres() {
        assumeTrue(postgresAvailable, "PostgreSQL 16 is required for running Flyway migration release gates.");
    }

    @Test
    @Order(1)
    @DisplayName("Gate 1: Run all migrations (V1-V7) against clean PostgreSQL schema")
    void testCleanMigrationV1ThroughV7() throws Exception {
        String cleanSchema = "scos_gate_clean_test";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP SCHEMA IF EXISTS " + cleanSchema + " CASCADE");
            stmt.execute("CREATE SCHEMA " + cleanSchema);
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dbUrl, dbUser, dbPassword)
                .schemas(cleanSchema)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        var result = flyway.migrate();
        assertTrue(result.migrationsExecuted >= 7, "All migrations V1 through V7 should have executed");

        MigrationInfo[] migrations = flyway.info().all();
        assertEquals(7, migrations.length, "Exactly 7 migration scripts should be discovered");

        for (MigrationInfo info : migrations) {
            assertEquals(MigrationState.SUCCESS, info.getState(),
                    "Migration " + info.getVersion() + " (" + info.getDescription() + ") must succeed");
        }

        // Verify key tables exist in the schema
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {
            assertTableExists(stmt, cleanSchema, "scos_communities");
            assertTableExists(stmt, cleanSchema, "scos_users");
            assertTableExists(stmt, cleanSchema, "scos_work_orders");
            assertTableExists(stmt, cleanSchema, "scos_work_order_sequences");
            assertTableExists(stmt, cleanSchema, "scos_assets");
            assertTableExists(stmt, cleanSchema, "scos_asset_status_history");
            assertTableExists(stmt, cleanSchema, "scos_work_order_attachments");

            // Verify V6 columns on scos_work_orders
            assertColumnExists(stmt, cleanSchema, "scos_work_orders", "work_order_type");
            assertColumnExists(stmt, cleanSchema, "scos_work_orders", "asset_id");
            assertColumnExists(stmt, cleanSchema, "scos_work_orders", "planned_start_at");
            assertColumnExists(stmt, cleanSchema, "scos_work_orders", "out_of_service");

            // Verify V7 attachment columns
            assertColumnExists(stmt, cleanSchema, "scos_work_order_attachments", "object_key");
            assertColumnExists(stmt, cleanSchema, "scos_work_order_attachments", "sha256_hex");
            assertColumnExists(stmt, cleanSchema, "scos_work_order_attachments", "community_id");
        }

        System.out.println(">>> [POSTGRES RELEASE GATE 1] Clean migration V1->V7 verified successfully.");
    }

    @Test
    @Order(2)
    @DisplayName("Gate 2: Test upgrading a database from V4 baseline through V7")
    void testUpgradeDatabaseFromV4ThroughV7() throws Exception {
        String upgradeSchema = "scos_gate_upgrade_test";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP SCHEMA IF EXISTS " + upgradeSchema + " CASCADE");
            stmt.execute("CREATE SCHEMA " + upgradeSchema);
        }

        // 1. Migrate up to V4 baseline
        Flyway flywayV4 = Flyway.configure()
                .dataSource(dbUrl, dbUser, dbPassword)
                .schemas(upgradeSchema)
                .locations("classpath:db/migration")
                .target("4")
                .cleanDisabled(false)
                .load();

        flywayV4.migrate();
        assertEquals("4", flywayV4.info().current().getVersion().getVersion(), "Baseline must be at V4");

        // 2. Perform rolling upgrade to V7
        Flyway flywayV7 = Flyway.configure()
                .dataSource(dbUrl, dbUser, dbPassword)
                .schemas(upgradeSchema)
                .locations("classpath:db/migration")
                .target("7")
                .load();

        var upgradeResult = flywayV7.migrate();
        assertEquals(3, upgradeResult.migrationsExecuted, "Exactly 3 migrations (V5, V6, V7) should execute during upgrade");
        assertEquals("7", flywayV7.info().current().getVersion().getVersion(), "Target must be at V7");

        System.out.println(">>> [POSTGRES RELEASE GATE 2] Database upgrade from V4 to V7 verified successfully.");
    }

    @Test
    @Order(3)
    @DisplayName("Gate 3: Verify attachment composite foreign keys and strict community isolation")
    void testAttachmentForeignKeysAndCommunityIsolation() throws Exception {
        String testSchema = "scos_gate_clean_test";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {

            stmt.execute("SET search_path TO " + testSchema + ", public");

            UUID communityA = UUID.randomUUID();
            UUID communityB = UUID.randomUUID();
            UUID userA = UUID.randomUUID();
            UUID userB = UUID.randomUUID();
            UUID workOrderA = UUID.randomUUID();

            // Insert Communities
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO scos_communities (id, code, name, address_line1, city, state_province, postal_code, status) VALUES (?, ?, ?, '123 Main St', 'Bengaluru', 'Karnataka', '560001', 'ACTIVE')")) {
                ps.setObject(1, communityA);
                ps.setString(2, "COMM_ALPHA_" + UUID.randomUUID().toString().substring(0, 8));
                ps.setString(3, "Community Alpha");
                ps.executeUpdate();

                ps.setObject(1, communityB);
                ps.setString(2, "COMM_BETA_" + UUID.randomUUID().toString().substring(0, 8));
                ps.setString(3, "Community Beta");
                ps.executeUpdate();
            }

            // Insert Users
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO scos_users (id, username, email, phone_number, password_hash, first_name, last_name, is_active) VALUES (?, ?, ?, ?, 'hash', ?, ?, true)")) {
                ps.setObject(1, userA);
                ps.setString(2, "user.alpha." + UUID.randomUUID().toString().substring(0, 6));
                ps.setString(3, "user.alpha@" + UUID.randomUUID().toString().substring(0, 6) + ".com");
                ps.setString(4, "+9198" + UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 8));
                ps.setString(5, "User");
                ps.setString(6, "Alpha");
                ps.executeUpdate();

                ps.setObject(1, userB);
                ps.setString(2, "user.beta." + UUID.randomUUID().toString().substring(0, 6));
                ps.setString(3, "user.beta@" + UUID.randomUUID().toString().substring(0, 6) + ".com");
                ps.setString(4, "+9197" + UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 8));
                ps.setString(5, "User");
                ps.setString(6, "Beta");
                ps.executeUpdate();
            }

            // Insert Work Order in Community Alpha
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO scos_work_orders (id, community_id, ticket_number, title, description, scope, category, priority, status, reported_by_user_id, sla_due_at, created_by, updated_by) " +
                            "VALUES (?, ?, 'WO-2026-TEST01', 'Elevator breakdown', 'Main passenger elevator stuck', 'COMMON_AREA', 'ELEVATOR', 'HIGH', 'OPEN', ?, CURRENT_TIMESTAMP + INTERVAL '24 hours', 'system', 'system')")) {
                ps.setObject(1, workOrderA);
                ps.setObject(2, communityA);
                ps.setObject(3, userA);
                ps.executeUpdate();
            }

            // Sub-test 3.1: Valid attachment insert within same Community Alpha
            UUID attachmentValid = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO scos_work_order_attachments (id, community_id, work_order_id, object_key, original_file_name, content_type, byte_size, uploaded_by_user_id) " +
                            "VALUES (?, ?, ?, 'attachments/alpha/elevator_panel.png', 'elevator_panel.png', 'image/png', 204850, ?)")) {
                ps.setObject(1, attachmentValid);
                ps.setObject(2, communityA);
                ps.setObject(3, workOrderA);
                ps.setObject(4, userA);
                int rows = ps.executeUpdate();
                assertEquals(1, rows, "Valid attachment matching community should insert successfully");
            }

            // Sub-test 3.2: Enforce Tenant Isolation - Mismatched community must fail FK constraint
            UUID attachmentInvalid = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO scos_work_order_attachments (id, community_id, work_order_id, object_key, original_file_name, content_type, byte_size, uploaded_by_user_id) " +
                            "VALUES (?, ?, ?, 'attachments/beta/leak.png', 'leak.png', 'image/png', 10240, ?)")) {
                ps.setObject(1, attachmentInvalid);
                ps.setObject(2, communityB); // MISMATCH: Community Beta attempting to attach to Alpha's work order!
                ps.setObject(3, workOrderA); // Alpha's work order
                ps.setObject(4, userB);

                SQLException ex = assertThrows(SQLException.class, ps::executeUpdate,
                        "Cross-tenant attachment insertion must be rejected by composite foreign key fk_scos_wo_attachment_work_order");
                // PostgreSQL foreign key violation SQLSTATE is 23503
                assertEquals("23503", ex.getSQLState(), "Expected PostgreSQL foreign_key_violation (23503)");
            }

            // Sub-test 3.3: Duplicate object_key in same community must fail unique constraint
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO scos_work_order_attachments (id, community_id, work_order_id, object_key, original_file_name, content_type, byte_size, uploaded_by_user_id) " +
                            "VALUES (?, ?, ?, 'attachments/alpha/elevator_panel.png', 'duplicate.png', 'image/png', 1024, ?)")) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, communityA);
                ps.setObject(3, workOrderA);
                ps.setObject(4, userA);

                SQLException ex = assertThrows(SQLException.class, ps::executeUpdate,
                        "Duplicate object_key in same community must be rejected by uq_scos_wo_attachment_object");
                // PostgreSQL unique violation SQLSTATE is 23505
                assertEquals("23505", ex.getSQLState(), "Expected PostgreSQL unique_violation (23505)");
            }

            // Sub-test 3.4: Same object_key in a DIFFERENT community must succeed (demonstrates tenant-partitioned keys)
            // First create a work order in Beta
            UUID workOrderB = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO scos_work_orders (id, community_id, ticket_number, title, description, scope, category, priority, status, reported_by_user_id, sla_due_at, created_by, updated_by) " +
                            "VALUES (?, ?, 'WO-2026-TEST02', 'Pump maintenance', 'Water pump service', 'COMMON_AREA', 'PLUMBING', 'NORMAL', 'OPEN', ?, CURRENT_TIMESTAMP + INTERVAL '24 hours', 'system', 'system')")) {
                ps.setObject(1, workOrderB);
                ps.setObject(2, communityB);
                ps.setObject(3, userB);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO scos_work_order_attachments (id, community_id, work_order_id, object_key, original_file_name, content_type, byte_size, uploaded_by_user_id) " +
                            "VALUES (?, ?, ?, 'attachments/alpha/elevator_panel.png', 'same_key_in_beta.png', 'image/png', 1024, ?)")) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, communityB); // Different community, same object_key
                ps.setObject(3, workOrderB);
                ps.setObject(4, userB);
                int rows = ps.executeUpdate();
                assertEquals(1, rows, "Identical object_key across different communities must succeed");
            }

            System.out.println(">>> [POSTGRES RELEASE GATE 3] Attachment foreign keys and tenant isolation verified successfully.");
        }
    }

    private void assertTableExists(Statement stmt, String schema, String tableName) throws SQLException {
        try (ResultSet rs = stmt.executeQuery(
                "SELECT 1 FROM information_schema.tables WHERE table_schema = '" + schema + "' AND table_name = '" + tableName + "'")) {
            assertTrue(rs.next(), "Table " + schema + "." + tableName + " must exist");
        }
    }

    private void assertColumnExists(Statement stmt, String schema, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = stmt.executeQuery(
                "SELECT 1 FROM information_schema.columns WHERE table_schema = '" + schema + "' AND table_name = '" + tableName + "' AND column_name = '" + columnName + "'")) {
            assertTrue(rs.next(), "Column " + schema + "." + tableName + "." + columnName + " must exist");
        }
    }
}
