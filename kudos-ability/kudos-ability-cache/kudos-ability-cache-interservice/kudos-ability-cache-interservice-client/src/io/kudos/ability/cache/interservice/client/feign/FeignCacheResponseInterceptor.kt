package io.kudos.ability.cache.interservice.client.feign

import feign.FeignException
import feign.Response
import feign.codec.Decoder
import io.kudos.ability.cache.interservice.client.core.ClientCacheHelper
import io.kudos.ability.cache.interservice.common.ClientCacheItem
import io.kudos.ability.cache.interservice.common.ClientCacheKey
import io.kudos.base.logger.LogFactory
import java.io.IOException
import java.lang.reflect.Type

/**
 * Feign 请求返回拦截器：
 * 当请求为 304 时，直接获取本地缓存结果
 */
class FeignCacheResponseInterceptor(
    private val delegate: Decoder,               // 真实的 Feign Decoder，由 Spring Cloud 配置好
    private val cacheHelper: ClientCacheHelper   // 直接构造注入，不再使用字段注入
) : Decoder {

    @Throws(IOException::class, FeignException::class)
    override fun decode(response: Response, type: Type): Any? {
        // 没开本地缓存，直接走原始 decoder
        if (!cacheHelper.havaLocalCache()) {
            return delegate.decode(response, type)
        }

        val headers = response.headers()

        // 如果 header 没有 cache-id 或 cache-status，就不走缓存逻辑
        if (!headers.containsKey(ClientCacheKey.HEADER_KEY_CACHE_UID) ||
            !headers.containsKey(ClientCacheKey.HEADER_KEY_CACHE_STATUS)
        ) {
            return delegate.decode(response, type)
        }

        // 获取本次请求的本地缓存 key：url+method+params
        val cacheKeys = response.request().headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY]
        val cacheKey = cacheKeys?.firstOrNull()
        if (cacheKey.isNullOrEmpty()) {
            return delegate.decode(response, type)
        }

        log.info("缓存Key：$cacheKey")

        // 获取 response 的 cache-status
        val status = headers[ClientCacheKey.HEADER_KEY_CACHE_STATUS]
        val cacheStatus = status?.firstOrNull()
        log.info("服务返回状态：$cacheStatus")

        // 真正的结果
        val result: Any? = if (ClientCacheKey.STATUS_USE_CACHE == cacheStatus) {
            // header 状态为 304，从本地缓存获取数据并返回
            val cacheItem = cacheHelper.loadFromLocalCache(cacheKey)
            cacheItem?.cacheData
        } else {
            // 否则，将本次数据放入本地缓存
            val decoded = delegate.decode(response, type)

            val cacheUids = headers[ClientCacheKey.HEADER_KEY_CACHE_UID]
            val cacheUid = cacheUids?.firstOrNull()
            if (!cacheUid.isNullOrEmpty()) {
                val cacheItem = ClientCacheItem(cacheUid, decoded)
                cacheHelper.writeToLocalCache(cacheKey, cacheItem)
            }

            decoded
        }

        return result
    }

    private val log = LogFactory.getLog(this)
}
