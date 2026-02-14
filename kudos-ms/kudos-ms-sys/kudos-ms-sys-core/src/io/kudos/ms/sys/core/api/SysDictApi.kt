package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysDictApi
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheItem
import io.kudos.ms.sys.core.service.iservice.ISysDictService
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * 字典 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
@Component
open class SysDictApi : ISysDictApi {
//endregion your codes 1

    //region your codes 2

    @Resource
    protected lateinit var sysDictService: ISysDictService

    override fun getActiveDictItems(
        dictType: String,
        atomicServiceCode: String?
    ): List<SysDictItemCacheItem> {
        return sysDictService.getActiveDictItems(dictType, atomicServiceCode)
    }

    override fun getActiveDictItemMap(
        dictType: String,
        atomicServiceCode: String?
    ): LinkedHashMap<String, String> {
        return sysDictService.getActiveDictItemMap(dictType, atomicServiceCode)
    }

    override fun batchGetActiveDictItems(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, List<SysDictItemCacheItem>> {
        return sysDictService.batchGetActiveDictItems(dictTypeAndASCodePairs)
    }

    override fun batchGetActiveDictItemMap(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, LinkedHashMap<String, String>> {
        return sysDictService.batchGetActiveDictItemMap(dictTypeAndASCodePairs)
    }

    //endregion your codes 2

}