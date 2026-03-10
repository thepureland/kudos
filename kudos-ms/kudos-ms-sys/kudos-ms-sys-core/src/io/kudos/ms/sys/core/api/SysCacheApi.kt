package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysCacheApi
import io.kudos.ms.sys.common.vo.cache.SysCacheCacheEntry
import io.kudos.ms.sys.common.vo.cache.SysCacheRow
import io.kudos.ms.sys.core.service.iservice.ISysCacheService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service


/**
 * 缓存 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@Service
open class SysCacheApi : ISysCacheApi {
//endregion your codes 1

    //region your codes 2

    @Resource
    protected lateinit var sysCacheService: ISysCacheService

    override fun getCacheFromCache(name: String): SysCacheCacheEntry? {
        return sysCacheService.getCacheFromCache(name)
    }

    override fun updateActive(id: String, active: Boolean): Boolean {
        return sysCacheService.updateActive(id, active)
    }

    override fun getCachesByAtomicServiceCode(atomicServiceCode: String): List<SysCacheRow> {
        return sysCacheService.getCachesByAtomicServiceCode(atomicServiceCode)
    }

    override fun getAllActiveCaches(): List<SysCacheRow> {
        return sysCacheService.getAllActiveCaches()
    }

    //endregion your codes 2

}