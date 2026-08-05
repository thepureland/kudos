package io.kudos.ability.log.audit.rdb.common

import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [AuditLogSchema].
 *
 * Coverage:
 *  - Every table-name / column-name constant equals its expected DDL identifier (exact value, lower-case snake_case).
 *  - Column constants within each table are unique (no duplicate column names by copy-paste).
 *  - Cross-check against the flyway DDL `db/migration/V20260519__create_sys_audit_log.sql` (this module's main
 *    resources, hence on the test classpath): the column set declared by each `CREATE TABLE` statement must equal
 *    the corresponding constant object's column set, and both table names must appear in the DDL — the KDoc claims
 *    this object is the "single source of truth" for the DDL identifiers, so any drift between the two must fail here.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AuditLogSchemaTest {

    @Test
    fun tableNameConstants() {
        assertEquals("sys_audit_log", AuditLogSchema.TABLE_AUDIT_LOG)
        assertEquals("sys_audit_detail_log", AuditLogSchema.TABLE_AUDIT_DETAIL_LOG)
    }

    @Test
    fun auditLogColumnConstants() {
        with(AuditLogSchema.AuditLogColumn) {
            assertEquals("id", ID)
            assertEquals("entity_id", ENTITY_ID)
            assertEquals("operate_type_id", OPERATE_TYPE_ID)
            assertEquals("operate_type", OPERATE_TYPE)
            assertEquals("module_id", MODULE_ID)
            assertEquals("module_name", MODULE_NAME)
            assertEquals("module_code", MODULE_CODE)
            assertEquals("description", DESCRIPTION)
            assertEquals("operator", OPERATOR)
            assertEquals("operator_id", OPERATOR_ID)
            assertEquals("operator_user_type", OPERATOR_USER_TYPE)
            assertEquals("tenant_id", TENANT_ID)
            assertEquals("source_tenant_id", SOURCE_TENANT_ID)
            assertEquals("sub_sys_code", SUB_SYS_CODE)
            assertEquals("operate_time", OPERATE_TIME)
            assertEquals("operate_ip", OPERATE_IP)
            assertEquals("operate_ip_dict_code", OPERATE_IP_DICT_CODE)
            assertEquals("client_os", CLIENT_OS)
            assertEquals("client_browser", CLIENT_BROWSER)
            assertEquals("request_type", REQUEST_TYPE)
        }
    }

    @Test
    fun auditDetailLogColumnConstants() {
        with(AuditLogSchema.AuditDetailLogColumn) {
            assertEquals("id", ID)
            assertEquals("audit_id", AUDIT_ID)
            assertEquals("operate_url", OPERATE_URL)
            assertEquals("string_params", STRING_PARAMS)
            assertEquals("object_params", OBJECT_PARAMS)
            assertEquals("request_referer", REQUEST_REFERER)
            assertEquals("request_form_data", REQUEST_FORM_DATA)
            assertEquals("description", DESCRIPTION)
        }
    }

    @Test
    fun columnConstantsAreUniqueAndWellFormed() {
        val identifierRegex = Regex("^[a-z][a-z0-9_]*$")
        listOf(
            constStringValues(AuditLogSchema.AuditLogColumn::class.java),
            constStringValues(AuditLogSchema.AuditDetailLogColumn::class.java)
        ).forEach { columns ->
            assertTrue(columns.isNotEmpty())
            assertEquals(columns.size, columns.toSet().size, "duplicate column names: $columns")
            columns.forEach {
                assertTrue(identifierRegex.matches(it), "not a lower-case snake_case identifier: '$it'")
            }
        }
    }

    @Test
    fun objectsAreSingletons() {
        // Touch the object instances so the (otherwise const-inlined) declarations are exercised at runtime.
        assertNotNull(AuditLogSchema)
        assertNotNull(AuditLogSchema.AuditLogColumn)
        assertNotNull(AuditLogSchema.AuditDetailLogColumn)
    }

    @Test
    fun ddlColumnsMatchAuditLogColumnConstants() {
        val ddl = loadDdl()
        assertEquals(
            ddlColumnsOf(ddl, AuditLogSchema.TABLE_AUDIT_LOG),
            constStringValues(AuditLogSchema.AuditLogColumn::class.java).toSet()
        )
    }

    @Test
    fun ddlColumnsMatchAuditDetailLogColumnConstants() {
        val ddl = loadDdl()
        assertEquals(
            ddlColumnsOf(ddl, AuditLogSchema.TABLE_AUDIT_DETAIL_LOG),
            constStringValues(AuditLogSchema.AuditDetailLogColumn::class.java).toSet()
        )
    }

    /** Reads the flyway migration script from this module's main resources (UTF-8, contains Chinese comments). */
    private fun loadDdl(): String {
        val path = "db/migration/V20260519__create_sys_audit_log.sql"
        val stream = javaClass.classLoader.getResourceAsStream(path)
        assertNotNull(stream, "DDL resource not found on classpath: $path")
        return stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    /** Extracts the column names declared by `CREATE TABLE IF NOT EXISTS <table> (...)` in [ddl]. */
    private fun ddlColumnsOf(ddl: String, table: String): Set<String> {
        val regex = Regex(
            """CREATE\s+TABLE\s+IF\s+NOT\s+EXISTS\s+$table\s*\((.*?)\)\s*;""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val body = regex.find(ddl)?.groupValues?.get(1)
        assertNotNull(body, "CREATE TABLE statement for '$table' not found in DDL")
        return body.lineSequence()
            .map { it.trim().trimEnd(',') }
            .filter { it.isNotEmpty() && !it.startsWith("PRIMARY KEY", ignoreCase = true) }
            .map { it.substringBefore(' ') }
            .toSet()
    }

    /** All `const val` String fields of [clazz] (compiled to public static final String). */
    private fun constStringValues(clazz: Class<*>): List<String> =
        clazz.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .map { it.get(null) as String }
}
