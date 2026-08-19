package io.kudos.ms.sys.client.microservice.fallback

import io.kudos.ability.distributed.client.http.fallback.AbstractHttpFallbackSupport
import io.kudos.ms.sys.client.microservice.proxy.ISysMicroServiceProxy
import io.kudos.ms.sys.common.microservice.vo.SysMicroServiceCacheEntry


/**
 * Microservice fallback implementation.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
open class SysMicroServiceFallback : AbstractHttpFallbackSupport("SysMicroServiceFallback"), ISysMicroServiceProxy {

    override fun getMicroServiceFromCache(code: String): SysMicroServiceCacheEntry? {
        warnRead("getMicroServiceFromCache", code)
        return null
    }

    override fun getAllMicroServicesFromCache(): List<SysMicroServiceCacheEntry> {
        warnRead("getAllMicroServicesFromCache")
        return emptyList()
    }

    override fun getMicroServicesExcludeAtomicFromCache(): List<SysMicroServiceCacheEntry> {
        warnRead("getMicroServicesExcludeAtomicFromCache")
        return emptyList()
    }

    override fun getAtomicServicesFromCache(): List<SysMicroServiceCacheEntry> {
        warnRead("getAtomicServicesFromCache")
        return emptyList()
    }

    override fun getSubMicroServicesFromCache(parentCode: String): List<SysMicroServiceCacheEntry> {
        warnRead("getSubMicroServicesFromCache", parentCode)
        return emptyList()
    }

    override fun getAtomicServicesByParentCodeFromCache(parentCode: String): List<SysMicroServiceCacheEntry> {
        warnRead("getAtomicServicesByParentCodeFromCache", parentCode)
        return emptyList()
    }

    override fun updateActive(code: String, active: Boolean): Boolean {
        errorWrite("updateActive", code, active)
        return false
    }
}
