package io.kudos.ability.log.audit.rdb.common

/**
 * Table-name / column-name constants for the RDB storage of audit logs.
 *
 * Extracted into `rdb-common` so that multiple ORM implementations (currently Ktorm, possibly JPA / MyBatis in the
 * future) share the same "DDL identifiers" — if the business side changes a table or column name, only this one place
 * needs editing and all ORM adapters follow along.
 *
 * The DDL itself lives in `resources/db/migration/V20260519__create_sys_audit_log.sql` — a flyway-based deployment
 * only needs to add this module's classpath to the flyway scanner's search path
 * (`spring.flyway.locations` includes `classpath:db/migration` by default).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object AuditLogSchema {

    /** Main table: one record per audit action. */
    const val TABLE_AUDIT_LOG: String = "sys_audit_log"

    /** Detail table: each [TABLE_AUDIT_LOG] record corresponds to 0..1 detail rows (URL / params / description). */
    const val TABLE_AUDIT_DETAIL_LOG: String = "sys_audit_detail_log"

    /** Column names of [TABLE_AUDIT_LOG]. */
    object AuditLogColumn {
        const val ID = "id"
        const val ENTITY_ID = "entity_id"
        const val OPERATE_TYPE_ID = "operate_type_id"
        const val OPERATE_TYPE = "operate_type"
        const val MODULE_ID = "module_id"
        const val MODULE_NAME = "module_name"
        const val MODULE_CODE = "module_code"
        const val DESCRIPTION = "description"
        const val OPERATOR = "operator"
        const val OPERATOR_ID = "operator_id"
        const val OPERATOR_USER_TYPE = "operator_user_type"
        const val TENANT_ID = "tenant_id"
        const val SOURCE_TENANT_ID = "source_tenant_id"
        const val SUB_SYS_CODE = "sub_sys_code"
        const val OPERATE_TIME = "operate_time"
        const val OPERATE_IP = "operate_ip"
        const val OPERATE_IP_DICT_CODE = "operate_ip_dict_code"
        const val CLIENT_OS = "client_os"
        const val CLIENT_BROWSER = "client_browser"
        const val REQUEST_TYPE = "request_type"
    }

    /** Column names of [TABLE_AUDIT_DETAIL_LOG]. */
    object AuditDetailLogColumn {
        const val ID = "id"
        const val AUDIT_ID = "audit_id"
        const val OPERATE_URL = "operate_url"
        const val STRING_PARAMS = "string_params"
        const val OBJECT_PARAMS = "object_params"
        const val REQUEST_REFERER = "request_referer"
        const val REQUEST_FORM_DATA = "request_form_data"
        const val DESCRIPTION = "description"
    }
}
