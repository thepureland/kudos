package io.kudos.ms.sys.core.api

import io.kudos.ms.sys.common.api.ISysDictApi
import io.kudos.ms.sys.common.vo.dictitem.SysDictItemCacheEntry
import io.kudos.ms.sys.core.service.iservice.ISysDictService
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * 字典 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysDictApi : ISysDictApi {


    @Resource
    protected lateinit var sysDictService: ISysDictService

    override fun getActiveDictItems(
        dictType: String,
        atomicServiceCode: String
    ): List<SysDictItemCacheEntry> {
        return sysDictService.getActiveDictItemsFromCache(dictType, atomicServiceCode)
    }

    override fun getActiveDictItemMap(
        dictType: String,
        atomicServiceCode: String
    ): LinkedHashMap<String, String> {
        return sysDictService.getActiveDictItemMapFromCache(dictType, atomicServiceCode)
    }

    override fun batchGetActiveDictItems(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, List<SysDictItemCacheEntry>> {
        return sysDictService.batchGetActiveDictItemsFromCache(dictTypeAndASCodePairs)
    }

    override fun batchGetActiveDictItemMap(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, LinkedHashMap<String, String>> {
        return sysDictService.batchGetActiveDictItemMapFromCache(dictTypeAndASCodePairs)
    }


}