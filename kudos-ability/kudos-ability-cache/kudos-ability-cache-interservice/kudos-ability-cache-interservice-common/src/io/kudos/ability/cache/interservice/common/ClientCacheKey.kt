package io.kudos.ability.cache.interservice.common

import java.io.Serial
import java.io.Serializable

class ClientCacheKey : Serializable {
    private var url: String? = null
    private var method: String? = null
    private var param: String? = null

    constructor()

    constructor(url: String?, method: String?, body: ByteArray?) {
        this.url = url
        this.method = method
        if (body != null && body.size > 0) {
            this.param = String(body)
        }
    }

    override fun toString(): String {
        return this.url + FEIGN_CACHE_DELIMITER + method + FEIGN_CACHE_DELIMITER + this.param
    }

    companion object {
        @Serial
        private val serialVersionUID = -5550889928680327059L

        //feign缓存的主key
        const val FEIGN_CACHE_PREFIX: String = "FEIGN-CACHE"

        //feign缓存分隔符
        const val FEIGN_CACHE_DELIMITER: String = "::"

        //feign请求，header的uid值
        const val HEADER_KEY_CACHE_UID: String = "cache-uid"

        //feign请求，当前请求的本地缓存cachekey
        const val HEADER_KEY_CACHE_KEY: String = "cache-key"

        //feign响应时，header的状态字段
        const val HEADER_KEY_CACHE_STATUS: String = "cache-status"

        //feign响应时，告知客户端使用缓存数据
        const val STATUS_USE_CACHE: String = "304"

        //feign响应时，告知客户端使用返回数据，并缓存本地
        const val STATUS_DO_CACHE: String = "200"
    }
}
