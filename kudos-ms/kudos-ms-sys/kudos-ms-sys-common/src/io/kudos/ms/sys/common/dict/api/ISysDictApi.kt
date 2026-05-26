package io.kudos.ms.sys.common.dict.api

import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * External dictionary API.
 *
 * Note: `batchGetActiveDictItems` / `batchGetActiveDictItemMap` use `Pair` parameters / `Map<Pair, V>` returns,
 * which do not serialize well via Jackson JSON, so they are **for in-process use only** and are not exposed via the Feign proxy.
 * For cross-service batch calls, issue multiple single GETs, or use the `List<List<String>>` adapter endpoints on [`SysDictInternalController`]
 * (`batchGetActiveDictItems` / `batchGetActiveDictItemMap`).
 *
 * @author K
 * @since 1.0.0
 */
interface ISysDictApi {


    /**
     * Fetch the dictionary items for the given dict type and atomic service code (active items only).
     */
    @GetMapping("/api/internal/sys/dict/getActiveDictItems")
    fun getActiveDictItems(
        @RequestParam dictType: String,
        @RequestParam atomicServiceCode: String
    ): List<SysDictItemCacheEntry>

    /**
     * Fetch dictionary item codes and names for the given dict type and atomic service code (active items only).
     */
    @GetMapping("/api/internal/sys/dict/getActiveDictItemMap")
    fun getActiveDictItemMap(
        @RequestParam dictType: String,
        @RequestParam atomicServiceCode: String
    ): LinkedHashMap<String, String>

    /**
     * Fetch dictionary item info for the given list of dict-type / atomic-service-code pairs (active items only).
     *
     * In-process only, not exposed via Feign (Pair / Map-by-Pair don't serialize well to JSON).
     */
    fun batchGetActiveDictItems(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, List<SysDictItemCacheEntry>>

    /**
     * Fetch dictionary item codes and names for the given list of dict-type / atomic-service-code pairs (active items only).
     *
     * In-process only, not exposed via Feign.
     */
    fun batchGetActiveDictItemMap(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, LinkedHashMap<String, String>>



}
