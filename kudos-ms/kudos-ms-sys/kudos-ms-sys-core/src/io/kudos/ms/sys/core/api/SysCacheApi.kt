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
@Service
open class SysCacheApi : ISysCacheApi {

    @Resource
    protected lateinit var sysCacheService: ISysCacheService





}