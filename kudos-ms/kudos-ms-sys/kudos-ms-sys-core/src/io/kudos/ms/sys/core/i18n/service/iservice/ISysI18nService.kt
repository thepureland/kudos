package io.kudos.ms.sys.core.i18n.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.i18n.vo.SysI18nCacheEntry
import io.kudos.ms.sys.common.i18n.vo.request.SysI18nFormUpdate
import io.kudos.ms.sys.core.i18n.model.po.SysI18n


/**
 * Business interface for i18n entries.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysI18nService : IBaseCrudService<String, SysI18n> {

    /**
     * Load a single i18n entry from the Hash cache by primary key (on miss, load from DB and write back).
     */
    fun getI18nFromCache(id: String): SysI18nCacheEntry?

    /**
     * Get the translation for a key in the given dimensions (from the map returned by [getI18nsFromCache]).
     */
    fun getI18nValueFromCache(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
        key: String
    ): String?

    /**
     * Load a key->translation map from the Hash cache by locale, type, namespace, and atomic service (only enabled entries participate in indexing).
     */
    fun getI18nsFromCache(
        locale: String,
        i18nTypeDictCode: String,
        namespace: String,
        atomicServiceCode: String,
    ): Map<String, String>

    /**
     * Batch-merge translations from the cache for multiple types and namespaces.
     */
    fun batchGetI18nsFromCache(
        locale: String,
        namespacesByI18nTypeDictCode: Map<String, Collection<String>>,
        atomicServiceCodes: Collection<String>,
    ): Map<String, Map<String, Map<String, String>>>

    /**
     * Batch save or update i18n entries.
     */
    fun batchSaveOrUpdate(i18ns: List<SysI18nFormUpdate>): Int

    /**
     * Update the active status and sync the Hash cache.
     */
    fun updateActive(id: String, active: Boolean): Boolean

}
