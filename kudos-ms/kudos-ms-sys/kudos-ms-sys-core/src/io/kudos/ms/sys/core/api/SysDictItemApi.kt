package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysDictItemApi
import io.kudos.ms.sys.core.service.iservice.ISysDictItemService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service


/**
 * 字典项 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Service
open class SysDictItemApi : ISysDictItemApi {


    @Resource
    protected lateinit var sysDictItemService: ISysDictItemService



}