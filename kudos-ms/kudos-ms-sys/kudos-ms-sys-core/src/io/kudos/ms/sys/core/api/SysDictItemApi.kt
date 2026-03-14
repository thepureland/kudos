package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysDictItemApi
import io.kudos.ms.sys.common.vo.dict.SysDictForm
import io.kudos.ms.sys.common.vo.dict.SysDictTreeNode
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemRow
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemQuery
import io.kudos.ms.sys.core.service.iservice.ISysDictItemService
import jakarta.annotation.Resource
import org.springframework.stereotype.Service


/**
 * 字典项 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@Service
open class SysDictItemApi : ISysDictItemApi {
//endregion your codes 1

    //region your codes 2

    @Resource
    protected lateinit var sysDictItemService: ISysDictItemService


    //endregion your codes 2

}