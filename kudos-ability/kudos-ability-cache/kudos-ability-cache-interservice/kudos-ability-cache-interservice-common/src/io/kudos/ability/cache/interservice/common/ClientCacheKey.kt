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
        return this.url + FEIGN_CACHE_DELIMITER + method + FEIGN_CACHE_DELIMITER + this.param
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = -5550889928680327059L

        // primary key for feign cache
        const val FEIGN_CACHE_PREFIX: String = "FEIGN-CACHE"

        // feign cache delimiter
        const val FEIGN_CACHE_DELIMITER: String = "::"

        // feign request header: uid value
        const val HEADER_KEY_CACHE_UID: String = "cache-uid"

        // feign request header: cache key of the current request's local cache
        const val HEADER_KEY_CACHE_KEY: String = "cache-key"

        // feign response header: status field
        const val HEADER_KEY_CACHE_STATUS: String = "cache-status"

        // feign response status: tells the client to use the cached data
        const val STATUS_USE_CACHE: String = "304"

        // feign response status: tells the client to use the returned data and cache it locally
        const val STATUS_DO_CACHE: String = "200"
    }
}
