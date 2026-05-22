package io.kudos.ability.log.audit.rdb.common

/**
 * 审计日志 RDB 存储的表名 / 列名常量。
 *
 * 抽到 `rdb-common` 是为了让多种 ORM 实现（当前 Ktorm，未来可能的 JPA / MyBatis）
 * 共享同一份"DDL 标识"——业务侧若改表名或列名，只需要在这一处改，所有 ORM 适配跟着走。
 *
 * DDL 本身见 `resources/db/migration/V20260519__create_sys_audit_log.sql`——
 * 走 flyway 的部署只需把本模块 classpath 加进 flyway scanner 的搜索路径
 * （`spring.flyway.locations` 默认包含 `classpath:db/migration`）。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object AuditLogSchema {

    /** 主表：一条审计动作记录。 */
    const val TABLE_AUDIT_LOG: String = "sys_audit_log"

    /** 详情表：一条 [TABLE_AUDIT_LOG] 记录对应 0..1 条详情（URL / 参数 / 描述）。 */
    const val TABLE_AUDIT_DETAIL_LOG: String = "sys_audit_detail_log"

    /** [TABLE_AUDIT_LOG] 列名。 */
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

    /** [TABLE_AUDIT_DETAIL_LOG] 列名。 */
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
