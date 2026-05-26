package io.kudos.ms.sys.core.system.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.system.vo.SysSystemCacheEntry
import io.kudos.ms.sys.core.system.model.po.SysSystem


/**
 * System service interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysSystemService : IBaseCrudService<String, SysSystem> {

    /**
     * Load system info by system code and cache the result.
     *
     * @param code system code (primary key), non-blank
     * @return cache entry, or null if not found
     */
    fun getSystemFromCache(code: String): SysSystemCacheEntry?

    /**
     * Fetch all systems from cache (including inactive).
     *
     * @return list of system cache entries
     */
    fun getAllSystemsFromCache(): List<SysSystemCacheEntry>

    /**
     * Fetch the list of top-level (non sub-) systems from cache, i.e. `subSystem == false` (including inactive).
     *
     * @return list of system cache entries
     */
    fun getSystemsExcludeSubSystemFromCache(): List<SysSystemCacheEntry>

    /**
     * Update the enabled state and sync the cache.
     *
     * @param code system code (primary key)
     * @param active whether enabled
     * @return whether the update succeeded
     */
    fun updateActive(code: String, active: Boolean): Boolean

    /**
     * Fetch the list of child systems for a given parent system from cache (matched by parentCode, including inactive).
     *
     * @param systemCode parent system code
     * @return list of child system cache entries
     */
    fun getSubSystemsFromCache(systemCode: String): List<SysSystemCacheEntry>

    /**
     * Return the full system tree (with hierarchy).
     *
     * @return list of system tree nodes (roots and their children)
     */
    fun getFullSystemTree(): List<IdAndNameTreeNode<String>>

    /**
     * Fetch all active sub-system codes from cache (active=true and subSystem=true).
     *
     * @return list of sub-system codes
     */
    fun getActiveSubSystemCodes(): List<String>

    /**
     * Fetch all active top-level system codes from cache (active=true and subSystem=false).
     *
     * @return list of top-level system codes
     */
    fun getActiveSystemCodes(): List<String>


}
