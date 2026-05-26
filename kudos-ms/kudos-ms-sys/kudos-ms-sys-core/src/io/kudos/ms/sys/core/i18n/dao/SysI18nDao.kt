package io.kudos.ms.sys.core.i18n.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.common.i18n.vo.SysI18nCacheEntry
import io.kudos.ms.sys.core.i18n.model.po.SysI18n
import io.kudos.ms.sys.core.i18n.model.table.SysI18ns
import org.springframework.stereotype.Repository


/**
 * Data access object for i18n entries.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
open class SysI18nDao : BaseCrudDao<String, SysI18n, SysI18ns>() {


    /**
     * Fetch enabled i18n entries by locale, type, namespace, and atomic service code (for cache).
     * When namespace is blank, it is not used as a filter; the query uses only locale, atomicServiceCode, and i18nTypeDictCode.
     *
     * @param locale language-region
     * @param atomicServiceCode atomic service code
     * @param i18nTypeDictCode i18n type dictionary code
     * @param namespace namespace, defaults to null; when null it is not used in the query
     * @return List<SysI18nCacheEntry>, empty list when nothing is found
     */
    open fun fetchActiveI18nsForCache(
        locale: String,
        atomicServiceCode: String,
        i18nTypeDictCode: String,
        namespace: String? = null
    ): List<SysI18nCacheEntry> {
        val criteria = Criteria.and(
            SysI18n::locale eq locale,
            SysI18n::i18nTypeDictCode eq i18nTypeDictCode,
            SysI18n::atomicServiceCode eq atomicServiceCode,
            SysI18n::active eq true
        )
        if (!namespace.isNullOrBlank()) {
            criteria.addAnd(SysI18n::namespace eq namespace)
        }
        return searchAs<SysI18nCacheEntry>(criteria)
    }

    /**
     * Fetch all enabled i18n entries (for cache).
     *
     * @return List<SysI18nCacheEntry>
     */
    open fun fetchAllActiveI18nsForCache(): List<SysI18nCacheEntry> {
        val criteria = Criteria(SysI18n::active eq true)
        return searchAs<SysI18nCacheEntry>(criteria)
    }


}
