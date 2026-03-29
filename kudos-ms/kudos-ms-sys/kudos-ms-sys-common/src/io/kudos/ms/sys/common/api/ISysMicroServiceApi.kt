package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceCacheEntry


/**
 * 微服务 对外API
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysMicroServiceApi {

    fun getMicroServiceFromCache(code: String): SysMicroServiceCacheEntry?

    fun getAllMicroServicesFromCache(): List<SysMicroServiceCacheEntry>

    fun getMicroServicesExcludeAtomicFromCache(): List<SysMicroServiceCacheEntry>

    fun getAtomicServicesFromCache(): List<SysMicroServiceCacheEntry>

    fun getSubMicroServicesFromCache(parentCode: String): List<SysMicroServiceCacheEntry>

    fun getAtomicServicesByParentCodeFromCache(parentCode: String): List<SysMicroServiceCacheEntry>

    fun updateActive(code: String, active: Boolean): Boolean


}
