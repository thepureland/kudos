package io.kudos.ability.log.audit.rdb.clickhouse.service

import io.kudos.ability.data.rdb.ktorm.datasource.currentDatabase
import io.kudos.ability.log.audit.common.api.IAuditLogReadOnlyService
import io.kudos.ability.log.audit.common.entity.AuditLogPage
import io.kudos.ability.log.audit.common.entity.AuditLogQuery
import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import io.kudos.ability.log.audit.rdb.common.AuditLogSchema
import io.kudos.ability.log.audit.rdb.common.AuditLogSchema.AuditDetailLogColumn
import io.kudos.ability.log.audit.rdb.common.AuditLogSchema.AuditLogColumn
import io.kudos.context.core.KudosContextHolder
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

/**
 * ClickHouse implementation of [IAuditLogReadOnlyService] — the read-side companion to
 * [RdbClickhouseAuditService].
 *
 * Why raw JDBC instead of ktorm DSL:
 *  - ClickHouse JDBC ships no dialect plugin for ktorm. Without a dialect, ktorm's
 *    `select(specific_column)`, `count(*)`, `totalRecordsInAllPages`, and `.limit(offset, size)`
 *    all fail with `translate(...) must not be null`. Pulling `ktorm-support-postgresql` as a
 *    surrogate dialect "works" but emits SQL features ClickHouse may not accept (RETURNING etc.)
 *    and feels wrong.
 *  - ClickHouse SQL is straightforward — `SELECT ... WHERE ... ORDER BY ... LIMIT ... OFFSET ...`
 *    are standard. Writing raw PreparedStatement is verbose but reliable and idiomatic for
 *    ClickHouse.
 *
 * Routes via [KudosContextHolder.currentDatabase] so multi-tenant deployments that pin audit
 * writes to a per-tenant ClickHouse instance also resolve reads against the right database.
 *
 * Ordering: every paged search returns rows by `operate_time DESC` — matches the ktorm/Mongo
 * impls and the typical admin "newest first" view.
 *
 * No `@Transactional` — ClickHouse has no rollback-able transactions, and the annotation would
 * be silently inert.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class RdbClickhouseAuditLogReadOnlyService : IAuditLogReadOnlyService {

    override fun findById(id: String): SysAuditLogVo? {
        return KudosContextHolder.currentDatabase().useConnection { conn ->
            conn.prepareStatement(SELECT_ALL_MAIN + " WHERE ${AuditLogColumn.ID} = ? LIMIT 1").use { ps ->
                ps.setString(1, id)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toMainVo() else null }
            }
        }
    }

    override fun findDetailById(auditId: String): SysAuditDetailLogVo? {
        return KudosContextHolder.currentDatabase().useConnection { conn ->
            conn.prepareStatement(SELECT_ALL_DETAIL + " WHERE ${AuditDetailLogColumn.AUDIT_ID} = ? LIMIT 1").use { ps ->
                ps.setString(1, auditId)
                ps.executeQuery().use { rs -> if (rs.next()) rs.toDetailVo() else null }
            }
        }
    }

    override fun pagingSearch(query: AuditLogQuery, pageNo: Int, pageSize: Int): AuditLogPage {
        val safePageNo = pageNo.coerceAtLeast(1)
        val safePageSize = pageSize.coerceAtLeast(1)

        val (whereClause, binders) = buildWhereClause(query)
        val db = KudosContextHolder.currentDatabase()

        val total = db.useConnection { conn ->
            conn.prepareStatement("SELECT count() FROM ${AuditLogSchema.TABLE_AUDIT_LOG} $whereClause").use { ps ->
                binders.forEachIndexed { i, bind -> bind(ps, i + 1) }
                ps.executeQuery().use { rs -> if (rs.next()) rs.getLong(1) else 0L }
            }
        }
        if (total == 0L) return AuditLogPage.empty(safePageNo, safePageSize)

        val offset = (safePageNo - 1) * safePageSize
        val items = db.useConnection { conn ->
            val sql = "$SELECT_ALL_MAIN $whereClause" +
                " ORDER BY ${AuditLogColumn.OPERATE_TIME} DESC, ${AuditLogColumn.ID} ASC" +
                " LIMIT $safePageSize OFFSET $offset"
            conn.prepareStatement(sql).use { ps ->
                binders.forEachIndexed { i, bind -> bind(ps, i + 1) }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<SysAuditLogVo>()
                    while (rs.next()) list += rs.toMainVo()
                    list
                }
            }
        }

        return AuditLogPage(items = items, total = total, pageNo = safePageNo, pageSize = safePageSize)
    }

    /**
     * Build the WHERE clause from non-null [AuditLogQuery] fields. Returns `("", [])` when no
     * filter applies — caller appends nothing to the FROM. Multiple non-null fields combine with
     * AND in declaration order.
     *
     * The second element of the pair is the list of `(ps, index) -> Unit` binders that match the
     * `?` placeholders in declaration order — caller iterates and invokes each at the right
     * statement index.
     */
    private fun buildWhereClause(query: AuditLogQuery): Pair<String, List<(PreparedStatement, Int) -> Unit>> {
        val clauses = mutableListOf<String>()
        val binders = mutableListOf<(PreparedStatement, Int) -> Unit>()

        query.tenantId?.let {
            clauses += "${AuditLogColumn.TENANT_ID} = ?"
            binders += { ps, i -> ps.setString(i, it) }
        }
        query.sourceTenantId?.let {
            clauses += "${AuditLogColumn.SOURCE_TENANT_ID} = ?"
            binders += { ps, i -> ps.setString(i, it) }
        }
        query.subSysCode?.let {
            clauses += "${AuditLogColumn.SUB_SYS_CODE} = ?"
            binders += { ps, i -> ps.setString(i, it) }
        }
        query.moduleCode?.let {
            clauses += "${AuditLogColumn.MODULE_CODE} = ?"
            binders += { ps, i -> ps.setString(i, it) }
        }
        query.operateTypeId?.let {
            clauses += "${AuditLogColumn.OPERATE_TYPE_ID} = ?"
            binders += { ps, i -> ps.setInt(i, it) }
        }
        query.operatorId?.let {
            clauses += "${AuditLogColumn.OPERATOR_ID} = ?"
            binders += { ps, i -> ps.setString(i, it) }
        }
        query.operatorUserType?.let {
            clauses += "${AuditLogColumn.OPERATOR_USER_TYPE} = ?"
            binders += { ps, i -> ps.setString(i, it) }
        }
        query.operatorLike?.takeIf { it.isNotEmpty() }?.let {
            clauses += "${AuditLogColumn.OPERATOR} LIKE ?"
            binders += { ps, i -> ps.setString(i, "%$it%") }
        }
        query.moduleCodeLike?.takeIf { it.isNotEmpty() }?.let {
            clauses += "${AuditLogColumn.MODULE_CODE} LIKE ?"
            binders += { ps, i -> ps.setString(i, "%$it%") }
        }
        query.operateType?.takeIf { it.isNotEmpty() }?.let {
            clauses += "${AuditLogColumn.OPERATE_TYPE} = ?"
            binders += { ps, i -> ps.setString(i, it) }
        }
        query.entityId?.let {
            clauses += "${AuditLogColumn.ENTITY_ID} = ?"
            binders += { ps, i -> ps.setString(i, it) }
        }
        // ClickHouse JDBC's setTimestamp ↔ DateTime64 round-trip is timezone-sensitive: the driver
        // adjusts for the connection's session timezone, which can shift values by hours vs how
        // the data was inserted literally. Passing the LocalDateTime as a yyyy-MM-dd HH:mm:ss
        // string and letting ClickHouse coerce sidesteps the TZ shim entirely — the comparison
        // happens in the server's local interpretation of both sides.
        query.operateTimeFrom?.let {
            clauses += "${AuditLogColumn.OPERATE_TIME} >= ?"
            binders += { ps, i -> ps.setString(i, it.format(CLICKHOUSE_TS_FORMAT)) }
        }
        query.operateTimeTo?.let {
            clauses += "${AuditLogColumn.OPERATE_TIME} < ?"
            binders += { ps, i -> ps.setString(i, it.format(CLICKHOUSE_TS_FORMAT)) }
        }
        query.descriptionLike?.takeIf { it.isNotEmpty() }?.let {
            clauses += "${AuditLogColumn.DESCRIPTION} LIKE ?"
            binders += { ps, i -> ps.setString(i, "%$it%") }
        }

        val whereClause = if (clauses.isEmpty()) "" else "WHERE " + clauses.joinToString(" AND ")
        return whereClause to binders
    }

    private fun ResultSet.toMainVo(): SysAuditLogVo = SysAuditLogVo().apply {
        id = getString(AuditLogColumn.ID)
        entityId = getString(AuditLogColumn.ENTITY_ID)
        operateTypeId = getNullableInt(AuditLogColumn.OPERATE_TYPE_ID)
        operateType = getString(AuditLogColumn.OPERATE_TYPE)
        moduleId = getNullableInt(AuditLogColumn.MODULE_ID)
        moduleName = getString(AuditLogColumn.MODULE_NAME)
        moduleCode = getString(AuditLogColumn.MODULE_CODE)
        description = getString(AuditLogColumn.DESCRIPTION)
        operator = getString(AuditLogColumn.OPERATOR)
        operatorId = getString(AuditLogColumn.OPERATOR_ID)
        operatorUserType = getString(AuditLogColumn.OPERATOR_USER_TYPE)
        tenantId = getString(AuditLogColumn.TENANT_ID)
        sourceTenantId = getString(AuditLogColumn.SOURCE_TENANT_ID)
        subSysCode = getString(AuditLogColumn.SUB_SYS_CODE)
        operateTime = getTimestamp(AuditLogColumn.OPERATE_TIME)?.toDate()
        operateIp = getNullableLong(AuditLogColumn.OPERATE_IP)
        operateIpDictCode = getString(AuditLogColumn.OPERATE_IP_DICT_CODE)
        clientOs = getString(AuditLogColumn.CLIENT_OS)
        clientBrowser = getString(AuditLogColumn.CLIENT_BROWSER)
        requestType = getString(AuditLogColumn.REQUEST_TYPE)
    }

    private fun ResultSet.toDetailVo(): SysAuditDetailLogVo = SysAuditDetailLogVo().apply {
        id = getString(AuditDetailLogColumn.ID)
        auditId = getString(AuditDetailLogColumn.AUDIT_ID)
        operateUrl = getString(AuditDetailLogColumn.OPERATE_URL)
        stringParams = getString(AuditDetailLogColumn.STRING_PARAMS)
        objectParams = getString(AuditDetailLogColumn.OBJECT_PARAMS)
        requestReferer = getString(AuditDetailLogColumn.REQUEST_REFERER)
        requestFormData = getString(AuditDetailLogColumn.REQUEST_FORM_DATA)
        description = getString(AuditDetailLogColumn.DESCRIPTION)
    }

    /** Read an INT column that may be SQL NULL. `getInt` returns 0 on null, which would lie. */
    private fun ResultSet.getNullableInt(column: String): Int? {
        val v = getInt(column)
        return if (wasNull()) null else v
    }

    /** Read a BIGINT column that may be SQL NULL. */
    private fun ResultSet.getNullableLong(column: String): Long? {
        val v = getLong(column)
        return if (wasNull()) null else v
    }

    private fun Timestamp.toDate(): Date = Date.from(toLocalDateTime().atZone(ZoneId.systemDefault()).toInstant())

    companion object {
        // ClickHouse DateTime literal format — yyyy-MM-dd HH:mm:ss. LocalDateTime.toString() omits
        // seconds when they're zero, which ClickHouse rejects on parse.
        private val CLICKHOUSE_TS_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        // Column-by-column mapping in `toMainVo` / `toDetailVo` reads by column NAME, so the
        // SELECT ordering doesn't matter for correctness — `SELECT *` is fine here.
        private const val SELECT_ALL_MAIN: String =
            "SELECT * FROM ${AuditLogSchema.TABLE_AUDIT_LOG}"
        private const val SELECT_ALL_DETAIL: String =
            "SELECT * FROM ${AuditLogSchema.TABLE_AUDIT_DETAIL_LOG}"
    }
}
