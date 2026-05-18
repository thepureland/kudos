package io.kudos.ms.sys.common.dict.api

import io.kudos.ms.sys.common.dict.vo.SysDictItemCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * 字典 对外API
 *
 * 说明：`batchGetActiveDictItems` / `batchGetActiveDictItemMap` 使用 `Pair` 入参/`Map<Pair, V>` 出参，
 * 不便走 Jackson JSON 序列化，故**仅供进程内调用**，Feign 代理不暴露。
 * 跨服务批量调用请直接多次 GET 单条；或通过 [`SysDictInternalController`] 的 `List<List<String>>`
 * 适配端点（`batchGetActiveDictItems` / `batchGetActiveDictItemMap`）。
 *
 * @author K
 * @since 1.0.0
 */
interface ISysDictApi {


    /**
     * 根据字典类型和原子服务编码，取得对应字典项(仅包括处于启用状态的)
     */
    @GetMapping("/api/internal/sys/dict/getActiveDictItems")
    fun getActiveDictItems(
        @RequestParam dictType: String,
        @RequestParam atomicServiceCode: String
    ): List<SysDictItemCacheEntry>

    /**
     * 根据字典类型和原子服务编码，取得对应字典项的编码和名称(仅包括处于启用状态的)
     */
    @GetMapping("/api/internal/sys/dict/getActiveDictItemMap")
    fun getActiveDictItemMap(
        @RequestParam dictType: String,
        @RequestParam atomicServiceCode: String
    ): LinkedHashMap<String, String>

    /**
     * 根据字典类型和原子服务编码列表，取得对应字典项信息(仅包括处于启用状态的)
     *
     * 仅进程内调用，不走 Feign（Pair/Map-by-Pair 不便走 JSON）。
     */
    fun batchGetActiveDictItems(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, List<SysDictItemCacheEntry>>

    /**
     * 根据字典类型和原子服务编码列表，取得对应字典项的编码和名称(仅包括处于启用状态的)
     *
     * 仅进程内调用，不走 Feign。
     */
    fun batchGetActiveDictItemMap(
        dictTypeAndASCodePairs: List<Pair<String, String>>
    ): Map<Pair<String, String>, LinkedHashMap<String, String>>



}
