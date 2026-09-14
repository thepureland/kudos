package io.kudos.ms.tag.core.schema

import com.mysql.cj.jdbc.MysqlDataSource
import io.kudos.ability.data.rdb.flyway.kit.FlywayKit
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.MySqlTestContainer
import io.kudos.test.container.containers.PostgresTestContainer
import org.h2.jdbcx.JdbcDataSource
import org.postgresql.ds.PGSimpleDataSource
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class TagSchemaContractTest {

    @Test
    fun h2SchemaHonorsPortableContracts() {
        val dataSource = JdbcDataSource().apply {
            setURL("jdbc:h2:mem:tag_${uniqueSuffix()};DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
            user = "sa"
            password = "sa"
        }

        verifySchema(dataSource)
    }

    @Test
    @EnabledIfDockerInstalled
    fun mysqlSchemaHonorsPortableContracts() {
        val container = MySqlTestContainer.startIfNeeded(null)
        val host = container.ports.first().ip
        val port = requireNotNull(container.ports.first().publicPort)
        val database = "tag_contract_${uniqueSuffix()}"
        val adminUrl = "jdbc:mysql://$host:$port/?useSSL=false&allowPublicKeyRetrieval=true"
        DriverManager.getConnection(adminUrl, MySqlTestContainer.USERNAME, MySqlTestContainer.PASSWORD).use {
            it.createStatement().use { statement -> statement.executeUpdate("create database $database") }
        }
        try {
            val dataSource = MysqlDataSource().apply {
                setURL("jdbc:mysql://$host:$port/$database?useSSL=false&allowPublicKeyRetrieval=true")
                user = MySqlTestContainer.USERNAME
                password = MySqlTestContainer.PASSWORD
            }
            verifySchema(dataSource)
        } finally {
            DriverManager.getConnection(adminUrl, MySqlTestContainer.USERNAME, MySqlTestContainer.PASSWORD).use {
                it.createStatement().use { statement -> statement.executeUpdate("drop database $database") }
            }
        }
    }

    @Test
    @EnabledIfDockerInstalled
    fun postgresqlSchemaHonorsPortableContracts() {
        val database = "tag_contract_${uniqueSuffix()}"
        val container = PostgresTestContainer.startIfNeeded(null, database)
        val dataSource = PGSimpleDataSource().apply {
            serverNames = arrayOf(container.ports.first().ip)
            portNumbers = intArrayOf(requireNotNull(container.ports.first().publicPort))
            databaseName = database
            user = PostgresTestContainer.USERNAME
            password = PostgresTestContainer.PASSWORD
        }

        verifySchema(dataSource)
    }

    private fun verifySchema(dataSource: DataSource) {
        FlywayKit.migrate("tag", dataSource)
        dataSource.connection.use { connection ->
            assertTables(connection)
            assertCriticalIndexes(connection)
            seedControlPlane(connection)
            assertUniqueConstraints(connection)
            assertForeignKeys(connection)
            assertRuntimeConstraints(connection)
        }
    }

    private fun assertTables(connection: Connection) {
        val actual = buildSet {
            connection.metaData.getTables(connection.catalog, null, null, arrayOf("TABLE")).use { rows ->
                while (rows.next()) {
                    rows.getString("TABLE_NAME").lowercase().takeIf { it.startsWith("tag_") }?.let(::add)
                }
            }
        }
        assertEquals(REQUIRED_TABLES.toSet(), actual)
    }

    private fun assertCriticalIndexes(connection: Connection) {
        CRITICAL_INDEXES.forEach { (table, expectedIndexes) ->
            val indexes = mutableMapOf<String, MutableList<Pair<Int, String>>>()
            connection.metaData.getIndexInfo(connection.catalog, null, table, false, false).use { rows ->
                while (rows.next()) {
                    val indexName = rows.getString("INDEX_NAME")?.lowercase() ?: continue
                    val columnName = rows.getString("COLUMN_NAME")?.lowercase() ?: continue
                    indexes.getOrPut(indexName) { mutableListOf() }
                        .add(rows.getInt("ORDINAL_POSITION") to columnName)
                }
            }
            val actual = indexes.mapValues { (_, columns) -> columns.sortedBy { it.first }.map { it.second } }
            expectedIndexes.forEach { (indexName, expectedColumns) ->
                assertEquals(expectedColumns, actual[indexName], "Missing or malformed critical index $indexName")
            }
        }
    }

    private fun seedControlPlane(connection: Connection) {
        connection.execute(
            """
            insert into tag_subject_type
                (id, code, name, owner_service_code, active, built_in, version, create_time, update_time)
            values
                ('00000000-0000-0000-0000-000000000001', 'estate.house', 'House', 'estate', true, false, 0, current_timestamp, current_timestamp)
            """
        )
        connection.execute(
            """
            insert into tag_attribute_definition
                (id, tenant_id, subject_type, code, name, value_type, cardinality, active, built_in, version, create_time, update_time)
            values
                ('00000000-0000-0000-0000-000000000002', 'tenant-a', 'estate.house', 'area', 'Area', 'DECIMAL', 'SINGLE', true, false, 0, current_timestamp, current_timestamp)
            """
        )
        connection.execute(
            """
            insert into tag_set
                (id, tenant_id, subject_type, code, name, cardinality, active, built_in, version, create_time, update_time)
            values
                ('00000000-0000-0000-0000-000000000003', 'tenant-a', 'estate.house', 'estate.kind', 'Kind', 'MULTIPLE', true, false, 0, current_timestamp, current_timestamp)
            """
        )
        connection.execute(
            """
            insert into tag_definition
                (id, tenant_id, subject_type, code, name, tag_set_id, set_priority, manual_assignable, active, built_in, version, create_time, update_time)
            values
                ('00000000-0000-0000-0000-000000000004', 'tenant-a', 'estate.house', 'estate.large', 'Large', '00000000-0000-0000-0000-000000000003', 10, true, true, false, 0, current_timestamp, current_timestamp)
            """
        )
        connection.execute(
            """
            insert into tag_rule
                (id, tenant_id, tag_id, rule_version, status, expression_version, checksum, create_time, update_time)
            values
                ('00000000-0000-0000-0000-000000000005', 'tenant-a', '00000000-0000-0000-0000-000000000004', 1, 'DRAFT', 1, 'checksum-1', current_timestamp, current_timestamp)
            """
        )
        connection.execute(
            """
            insert into tag_rule_node
                (id, rule_id, node_kind, order_num)
            values
                ('00000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000005', 'ALL_OF', 0)
            """
        )
    }

    private fun assertUniqueConstraints(connection: Connection) {
        connection.expectConstraintViolation(
            """
            insert into tag_subject_type
                (id, code, name, owner_service_code, active, built_in, version, create_time, update_time)
            values
                ('10000000-0000-0000-0000-000000000001', 'estate.house', 'Duplicate', 'estate', true, false, 0, current_timestamp, current_timestamp)
            """
        )
        connection.expectConstraintViolation(
            """
            insert into tag_attribute_definition
                (id, tenant_id, subject_type, code, name, value_type, cardinality, active, built_in, version, create_time, update_time)
            values
                ('10000000-0000-0000-0000-000000000002', 'tenant-a', 'estate.house', 'area', 'Duplicate', 'DECIMAL', 'SINGLE', true, false, 0, current_timestamp, current_timestamp)
            """
        )
    }

    private fun assertForeignKeys(connection: Connection) {
        connection.expectConstraintViolation(
            """
            insert into tag_rule_node (id, rule_id, parent_id, node_kind, order_num)
            values ('10000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000005', 'ffffffff-ffff-ffff-ffff-ffffffffffff', 'NOT', 1)
            """
        )
        connection.expectConstraintViolation(
            """
            insert into tag_rule_dependency
                (id, tenant_id, rule_id, tag_id, dependency_type, attribute_id)
            values
                ('10000000-0000-0000-0000-000000000007', 'tenant-a', '00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000004', 'ATTRIBUTE', 'ffffffff-ffff-ffff-ffff-ffffffffffff')
            """
        )
    }

    private fun assertRuntimeConstraints(connection: Connection) {
        connection.execute(
            """
            insert into tag_subject
                (tenant_id, subject_type, subject_id, state_version, first_seen_time, update_time)
            values
                ('tenant-a', 'estate.house', 'house-1', 0, current_timestamp, current_timestamp)
            """
        )
        val eventSql =
            """
            insert into tag_attribute_event
                (event_id, payload_checksum, tenant_id, subject_type, subject_id, attribute_id, operation, source_code, occurred_time, received_time, process_status)
            values
                ('00000000-0000-0000-0000-000000000010', 'event-checksum', 'tenant-a', 'estate.house', 'house-1', '00000000-0000-0000-0000-000000000002', 'SET', 'estate', current_timestamp, current_timestamp, 'APPLIED')
            """
        connection.execute(eventSql)
        connection.expectConstraintViolation(eventSql)

        val membershipSql =
            """
            insert into tag_membership
                (id, tenant_id, subject_type, subject_id, tag_id, source_type, source_ref, active, membership_version, update_time)
            values
                ('00000000-0000-0000-0000-000000000011', 'tenant-a', 'estate.house', 'house-1', '00000000-0000-0000-0000-000000000004', 'MANUAL', 'admin-1', true, 1, current_timestamp)
            """
        connection.execute(membershipSql)
        connection.expectConstraintViolation(
            membershipSql.replace("00000000-0000-0000-0000-000000000011", "10000000-0000-0000-0000-000000000011")
        )

        val assignmentSql =
            """
            insert into tag_assignment
                (tenant_id, subject_type, subject_id, tag_id, assignment_version, materialized_time, update_time)
            values
                ('tenant-a', 'estate.house', 'house-1', '00000000-0000-0000-0000-000000000004', 1, current_timestamp, current_timestamp)
            """
        connection.execute(assignmentSql)
        connection.expectConstraintViolation(assignmentSql)

        connection.expectConstraintViolation(
            """
            insert into tag_definition
                (id, tenant_id, subject_type, code, name, set_priority, manual_assignable, active, built_in, version, create_time, update_time)
            values
                ('10000000-0000-0000-0000-000000000004', 'tenant-a', 'estate.house', 'estate.invalid', 'Invalid', -1, true, true, false, 0, current_timestamp, current_timestamp)
            """
        )
        connection.expectConstraintViolation(
            """
            insert into tag_recalculation_job
                (id, job_key, tenant_id, job_type, tag_id, rule_version, subject_type, status, priority, requested_version, processed_version, attempt_count, max_attempts, available_time, processed_count, create_time, update_time, version)
            values
                ('00000000-0000-0000-0000-000000000012', 'bad-version-job', 'tenant-a', 'SUBJECT_INCREMENTAL', '00000000-0000-0000-0000-000000000004', 1, 'estate.house', 'PENDING', 0, 1, 2, 0, 3, current_timestamp, 0, current_timestamp, current_timestamp, 0)
            """
        )
    }

    private fun Connection.execute(sql: String) {
        createStatement().use { statement -> statement.executeUpdate(sql.trimIndent()) }
    }

    private fun Connection.expectConstraintViolation(sql: String) {
        assertFailsWith<SQLException> {
            createStatement().use { statement -> statement.executeUpdate(sql.trimIndent()) }
        }
    }

    private fun uniqueSuffix(): String = UUID.randomUUID().toString().replace("-", "")

    private companion object {
        val REQUIRED_TABLES = listOf(
            "tag_subject_type",
            "tag_attribute_definition",
            "tag_set",
            "tag_definition",
            "tag_rule",
            "tag_rule_node",
            "tag_rule_operand",
            "tag_rule_dependency",
            "tag_taxonomy_node",
            "tag_taxonomy_tag",
            "tag_subject",
            "tag_attribute_event",
            "tag_attribute_state",
            "tag_membership",
            "tag_assignment",
            "tag_assignment_event",
            "tag_manual_assignment_event",
            "tag_recalculation_candidate",
            "tag_recalculation_job",
        )

        val CRITICAL_INDEXES = mapOf(
            "tag_attribute_state" to mapOf(
                "idx_tag_attribute_state_subject" to listOf("tenant_id", "subject_type", "attribute_id", "subject_id")
            ),
            "tag_membership" to mapOf(
                "idx_tag_membership_lookup" to listOf("tenant_id", "subject_type", "tag_id", "active", "subject_id")
            ),
            "tag_assignment" to mapOf(
                "idx_tag_assignment_lookup" to listOf("tenant_id", "subject_type", "tag_id", "subject_id")
            ),
            "tag_rule_dependency" to mapOf(
                "idx_tag_rule_dependency_attribute" to listOf("tenant_id", "dependency_type", "attribute_id", "rule_id"),
                "idx_tag_rule_dependency_tag" to listOf("tenant_id", "dependency_type", "referenced_tag_id", "rule_id"),
            ),
            "tag_recalculation_job" to mapOf(
                "idx_tag_recalculation_job_available" to listOf("status", "available_time", "priority", "id")
            ),
            "tag_recalculation_candidate" to mapOf(
                "idx_tag_recalculation_candidate_lookup" to listOf("run_id", "subject_type", "subject_id", "tag_id")
            ),
        )
    }
}
