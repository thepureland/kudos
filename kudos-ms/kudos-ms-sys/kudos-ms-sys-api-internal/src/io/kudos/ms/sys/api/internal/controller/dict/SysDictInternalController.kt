package io.kudos.ms.sys.api.internal.controller.dict

import io.kudos.ms.sys.common.dict.api.ISysDictApi
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.api.SysDictApi
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


/**
 * 字典 内部 RPC 控制器。
 *
 * 单条接口路径继承自 [ISysDictApi] 方法级注解；
 * 批量接口（入参为 `List<Pair>`、出参为 `Map<Pair, V>`）不便走 JSON 序列化，
 * 故额外提供本类自带的 `List<List<String>>` 适配端点供跨服务调用。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class SysDictInternalController(
    private val sysDictApi: SysDictApi,
) : ISysDictApi {

    override fun getActiveDictItems(
        dictType: String,
        atomicServiceCode: String,
    ): List<SysDictItemCacheEntry> = sysDictApi.getActiveDictItems(dictType, atomicServiceCode)

    override fun getActiveDictItemMap(
        dictType: String,
        atomicServiceCode: String,
    ): LinkedHashMap<String, String> = sysDictApi.getActiveDictItemMap(dictType, atomicServiceCode)

    override fun batchGetActiveDictItems(
        dictTypeAndASCodePairs: List<Pair<String, String>>,
    ): Map<Pair<String, String>, List<SysDictItemCacheEntry>> =
        sysDictApi.batchGetActiveDictItems(dictTypeAndASCodePairs)

    override fun batchGetActiveDictItemMap(
        dictTypeAndASCodePairs: List<Pair<String, String>>,
    ): Map<Pair<String, String>, LinkedHashMap<String, String>> =
        sysDictApi.batchGetActiveDictItemMap(dictTypeAndASCodePairs)

    /** Pair 不便走 JSON 的批量适配：用 `List<List<String>>` 表示 `[dictType, atomicServiceCode]` 对，返回 key 拼成 `"dictType|atomicServiceCode"`。 */
    @PostMapping("/api/internal/sys/dict/batchGetActiveDictItems")
    fun batchGetActiveDictItemsHttp(
        @RequestBody pairs: List<List<String>>,
    ): Map<String, List<SysDictItemCacheEntry>> {
        val tupled = pairs.map { it[0] to it[1] }
        return sysDictApi.batchGetActiveDictItems(tupled).mapKeys { "${it.key.first}|${it.key.second}" }
    }

    @PostMapping("/api/internal/sys/dict/batchGetActiveDictItemMap")
    fun batchGetActiveDictItemMapHttp(
        @RequestBody pairs: List<List<String>>,
    ): Map<String, LinkedHashMap<String, String>> {
        val tupled = pairs.map { it[0] to it[1] }
        return sysDictApi.batchGetActiveDictItemMap(tupled).mapKeys { "${it.key.first}|${it.key.second}" }
    }

}
