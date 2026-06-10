package io.kudos.ms.sys.api.internal.controller.dict

import io.kudos.ms.sys.common.dict.api.ISysDictApi
import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import io.kudos.ms.sys.core.dict.api.SysDictApi
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


/**
 * Dictionary internal RPC controller.
 *
 * Paths of single-record APIs are inherited from method-level annotations on [ISysDictApi];
 * batch APIs (inputs of `List<Pair>` and outputs of `Map<Pair, V>`) are inconvenient for JSON serialization,
 * so this class additionally exposes `List<List<String>>` adapter endpoints for cross-service calls.
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

    /**
     * Batch adapter for Pair, which is inconvenient over JSON: uses `List<List<String>>` to represent
     * `[dictType, atomicServiceCode]` pairs.
     *
     * Note the key order of the returned map: the in-process API keys its result by the **flipped** pair
     * `(atomicServiceCode, dictType)` (see `SysDictService.dictCacheKey`), so the concatenated keys here are
     * `"atomicServiceCode|dictType"` — reversed relative to the input element order.
     */
    @PostMapping("/api/internal/sys/dict/batchGetActiveDictItems")
    fun batchGetActiveDictItemsHttp(
        @RequestBody pairs: List<List<String>>,
    ): Map<String, List<SysDictItemCacheEntry>> {
        val tupled = pairs.map { it[0] to it[1] }
        return sysDictApi.batchGetActiveDictItems(tupled).mapKeys { "${it.key.first}|${it.key.second}" }
    }

    /**
     * Batch adapter returning the code->name mapping per dictionary; same input convention and
     * `"atomicServiceCode|dictType"` key order as [batchGetActiveDictItemsHttp].
     */
    @PostMapping("/api/internal/sys/dict/batchGetActiveDictItemMap")
    fun batchGetActiveDictItemMapHttp(
        @RequestBody pairs: List<List<String>>,
    ): Map<String, LinkedHashMap<String, String>> {
        val tupled = pairs.map { it[0] to it[1] }
        return sysDictApi.batchGetActiveDictItemMap(tupled).mapKeys { "${it.key.first}|${it.key.second}" }
    }

}
