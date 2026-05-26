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
 * Access rule DAO.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class SysAccessRuleDao : BaseCrudDao<String, SysAccessRule, SysAccessRules>() {

    /**
     * Convert a single entity into a [SysAccessRuleCacheEntry]; when the DB's `tenant_id` is null/blank, [SysAccessRuleCacheEntry.tenantId] becomes `""`,
     * matching the convention of `AccessRuleTenantKey.normalize` in the cache layer.
     *
     * @param entity DB entity, non-null
     * @return cache value object
     */
    private fun toCacheEntry(entity: SysAccessRule): SysAccessRuleCacheEntry =
        SysAccessRuleCacheEntry(
            id = entity.id,
            tenantId = entity.tenantId.trim(),
            systemCode = entity.systemCode.trim(),
            accessRuleTypeDictCode = entity.accessRuleTypeDictCode.trim(),
        )

    /**
     * Load a [SysAccessRuleCacheEntry] by primary key.
     *
     * @param id primary key
     * @return the cache entry if found, otherwise `null`
     */
    open fun fetchCacheEntryById(id: String): SysAccessRuleCacheEntry? {
        val e = get(id) ?: return null
        return toCacheEntry(e)
    }

    /**
     * Load a cache entry by system code and tenant dimension; when [tenantId] is null/blank, matches `tenant_id IS NULL` (platform-level).
     *
     * @param systemCode system code
     * @param tenantId tenant id; null/blank means platform-level
     * @return at most one row, `null` if not found
     */
    open fun fetchCacheEntryBySystemCodeAndTenantId(systemCode: String, tenantId: String?): SysAccessRuleCacheEntry? {
        val sc = systemCode.trim()
        if (sc.isEmpty()) return null
        val tid = tenantId?.trim()?.takeIf { it.isNotEmpty() }
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
     * Load [SysAccessRuleCacheEntry] for every access rule (for Hash cache full reload).
     *
     * @return list with size equal to the row count in the table
     */
    open fun fetchAllCacheEntries(): List<SysAccessRuleCacheEntry> =
        search(criteria = null).map { toCacheEntry(it) }

    /**
     * Query primary keys by sub-system code and tenant dimension.
     * When [tenantId] is null or blank, matches `tenant_id IS NULL` (platform-level access rules).
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
