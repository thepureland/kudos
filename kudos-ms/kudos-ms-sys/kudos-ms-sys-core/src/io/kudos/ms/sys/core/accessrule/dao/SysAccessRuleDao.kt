package io.kudos.ms.sys.core.accessrule.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.isNull
import io.kudos.ms.sys.common.accessrule.vo.SysAccessRuleCacheEntry
import io.kudos.ms.sys.core.accessrule.model.po.SysAccessRule
import io.kudos.ms.sys.core.accessrule.model.table.SysAccessRules
import org.springframework.stereotype.Repository


/**
 * 访问规则数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class SysAccessRuleDao : BaseCrudDao<String, SysAccessRule, SysAccessRules>() {

    /**
     * 将单行实体转为 [SysAccessRuleCacheEntry]；库中 `tenant_id` 为空时 [SysAccessRuleCacheEntry.tenantId] 为 `""`。
     *
     * @param entity 库表实体，非 null
     * @return 缓存值对象
     */
    private fun toCacheEntry(entity: SysAccessRule): SysAccessRuleCacheEntry {
        val tid = entity.tenantId
        val normalizedTenant = if (tid.isBlank()) "" else tid.trim()
        return SysAccessRuleCacheEntry(
            id = entity.id,
            tenantId = normalizedTenant,
            systemCode = entity.systemCode.trim(),
            accessRuleTypeDictCode = entity.accessRuleTypeDictCode.trim(),
        )
    }

    /**
     * 按主键加载 [SysAccessRuleCacheEntry]。
     *
     * @param id 主键
     * @return 存在时返回缓存项，否则 `null`
     */
    open fun fetchCacheEntryById(id: String): SysAccessRuleCacheEntry? {
        val e = get(id) ?: return null
        return toCacheEntry(e)
    }

    /**
     * 按系统编码与租户维度加载缓存项；[tenantId] 为空串时匹配 `tenant_id IS NULL`。
     *
     * @param systemCode 系统编码
     * @param tenantId 租户 id，空串表示平台级
     * @return 最多一行，不存在时 `null`
     */
    open fun fetchCacheEntryBySystemCodeAndtenantId(systemCode: String, tenantId: String): SysAccessRuleCacheEntry? {
        val sc = systemCode.trim()
        if (sc.isEmpty()) return null
        val tid = tenantId.trim().takeIf { it.isNotEmpty() }
        val criteria = if (tid == null) {
            Criteria.and(
                SysAccessRule::systemCode eq sc,
                SysAccessRule::tenantId.isNull(),
            )
        } else {
            Criteria.and(
                SysAccessRule::systemCode eq sc,
                SysAccessRule::tenantId eq tid,
            )
        }
        val e = search(criteria).firstOrNull() ?: return null
        return toCacheEntry(e)
    }

    /**
     * 加载全部访问规则对应的 [SysAccessRuleCacheEntry]（供 Hash 缓存全量刷新）。
     *
     * @return 与表中行数一致的列表
     */
    open fun fetchAllCacheEntries(): List<SysAccessRuleCacheEntry> =
        search(criteria = null).map { toCacheEntry(it) }

    /**
     * 按子系统编码与租户维度查询主键。
     * [tenantId] 为空或空白时匹配 `tenant_id IS NULL`（平台级访问规则）。
     */
    open fun findIdBySystemCodeAndTenantId(systemCode: String, tenantId: String?): String? {
        val sc = systemCode.trim()
        if (sc.isEmpty()) return null
        val criteria = if (tenantId.isNullOrBlank()) {
            Criteria.and(
                SysAccessRule::systemCode eq sc,
                SysAccessRule::tenantId.isNull(),
            )
        } else {
            Criteria.and(
                SysAccessRule::systemCode eq sc,
                SysAccessRule::tenantId eq tenantId.trim(),
            )
        }
        return search(criteria).firstOrNull()?.id
    }
}
