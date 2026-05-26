package io.kudos.ms.sys.core.microservice.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.base.tree.IdAndNameTreeNode
import io.kudos.ms.sys.common.microservice.vo.SysMicroServiceCacheEntry
import io.kudos.ms.sys.core.microservice.model.po.SysMicroService


/**
 * Microservice service interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysMicroServiceService : IBaseCrudService<String, SysMicroService> {

    /**
     * Load microservice info by code and cache the result.
     *
     * @param code Microservice code (primary key), non-blank
     * @return Cache entry, or null if not found
     */
    fun getMicroServiceFromCache(code: String): SysMicroServiceCacheEntry?

    /**
     * Get all microservices from cache (including atomic services and inactive ones).
     *
     * @return List of microservice cache entries
     */
    fun getAllMicroServicesFromCache(): List<SysMicroServiceCacheEntry>

    /**
     * Get non-atomic microservices from cache, i.e. `atomicService == false` (includes inactive).
     *
     * @return List of microservice cache entries
     */
    fun getMicroServicesExcludeAtomicFromCache(): List<SysMicroServiceCacheEntry>

    /**
     * Get all atomic microservices from cache (`atomicService == true`, includes inactive).
     *
     * @return List of microservice cache entries
     */
    fun getAtomicServicesFromCache(): List<SysMicroServiceCacheEntry>

    /**
     * Get microservices under the given parent code from cache (matched by parentCode, includes inactive).
     *
     * @param parentCode Parent microservice code
     * @return List of child microservice cache entries
     */
    fun getSubMicroServicesFromCache(parentCode: String): List<SysMicroServiceCacheEntry>

    /**
     * Get atomic microservices under the given parent code from cache (`parentCode` match and `atomicService == true`, includes inactive).
     *
     * @param parentCode Parent microservice code
     * @return List of atomic microservice cache entries
     */
    fun getAtomicServicesByParentCodeFromCache(parentCode: String): List<SysMicroServiceCacheEntry>

    /**
     * Return the full microservice tree (with hierarchy).
     *
     * @return List of microservice tree nodes (roots and their children)
     */
    fun getFullMicroServiceTree(): List<IdAndNameTreeNode<String>>

    /**
     * Update active flag and sync cache.
     *
     * @param code Microservice code (primary key)
     * @param active Whether active
     * @return Whether update succeeded
     */
    fun updateActive(code: String, active: Boolean): Boolean

    /**
     * Get all active atomic service codes from cache (`atomicService=true` and `active=true`).
     *
     * @return List of atomic service codes
     */
    fun getActiveAtomicServiceCodes(): List<String>

    /**
     * Get all active non-atomic microservice codes from cache (`atomicService=false` and `active=true`).
     *
     * @return List of microservice codes
     */
    fun getActiveMicroServiceCodes(): List<String>


}
