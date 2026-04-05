package io.kudos.ms.sys.core.dict.api
import io.kudos.ms.sys.common.dict.api.ISysDictApi
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.service.iservice.ISysDictService
import org.springframework.stereotype.Component


/**
 * 字典 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class SysDictApi(
    private val sysDictService: ISysDictService,
) : ISysDictApi {

    override fun getActiveDictItems(
        dictType: String,
        atomicServiceCode: String
    ): List<SysDictItemCacheEntry> = sysDictService.getActiveDictItemsFromCache(dictType, atomicServiceCode)

    override fun getActiveDictItemMap(
        dictType: String,
        atomicServiceCode: String
    ): LinkedHashMap<String, String> = sysDictService.getActiveDictItemMapFromCache(dictType, atomicServiceCode)

    override fun batchGetActiveDictItems(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, List<SysDictItemCacheEntry>> =
        sysDictService.batchGetActiveDictItemsFromCache(dictTypeAndASCodePairs)

    override fun batchGetActiveDictItemMap(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, LinkedHashMap<String, String>> =
        sysDictService.batchGetActiveDictItemMapFromCache(dictTypeAndASCodePairs)
}
