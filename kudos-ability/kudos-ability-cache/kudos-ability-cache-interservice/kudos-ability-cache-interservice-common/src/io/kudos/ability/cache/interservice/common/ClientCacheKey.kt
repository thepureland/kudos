package io.kudos.ability.cache.interservice.common

import java.io.Serial
import java.io.Serializable

/**
 * Client-side cache key.
 * Generates a unique key for inter-service caching based on URL, HTTP method, and request parameters.
 */
class ClientCacheKey : Serializable {
    private var url: String? = null
    private var method: String? = null
    private var param: String? = null

    constructor()

    constructor(url: String?, method: String?, body: ByteArray?) {
        this.url = url
        this.method = method
        if (body != null && body.isNotEmpty()) {
            this.param = String(body)
        }
    }

    override fun toString(): String {
        return this.url + RPC_CACHE_DELIMITER + method + RPC_CACHE_DELIMITER + this.param
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = -5550889928680327059L

        // 本地协商缓存的 region 名
        const val RPC_CACHE_PREFIX: String = "RPC-CACHE"

        // 缓存 key 各段之间的分隔符
        const val RPC_CACHE_DELIMITER: String = "::"

        // 请求头：本地副本的内容指纹
        const val HEADER_KEY_CACHE_UID: String = "cache-uid"

        // 请求头：本次请求的客户端本地缓存 key
        const val HEADER_KEY_CACHE_KEY: String = "cache-key"

        // 响应头：协商结果
        const val HEADER_KEY_CACHE_STATUS: String = "cache-status"

        // 协商结果：让客户端用本地缓存
        const val STATUS_USE_CACHE: String = "304"

        // 协商结果：让客户端用返回的数据并写入本地缓存
        const val STATUS_DO_CACHE: String = "200"
    }
}
