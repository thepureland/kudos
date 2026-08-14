package io.kudos.ability.cache.interservice.client.feign

import feign.FeignException
import feign.Response
import feign.codec.DecodeException
import feign.codec.Decoder
import io.kudos.ability.cache.interservice.client.core.ClientCacheHelper
import io.kudos.ability.cache.interservice.common.ClientCacheItem
import io.kudos.ability.cache.interservice.common.ClientCacheKey
import io.kudos.base.logger.LogFactory
import java.io.IOException
import java.lang.reflect.Type

/**
 * Feign response interceptor:
 * when the response is 304, returns the local cache result directly.
 */
class FeignCacheResponseInterceptor(
    private val delegate: Decoder,               // the real Feign Decoder, configured by Spring Cloud
    private val cacheHelper: ClientCacheHelper   // constructor-injected; no field injection
) : Decoder {

    @Throws(IOException::class, FeignException::class)
    override fun decode(response: Response, type: Type): Any? {
        // Local cache is disabled: fall through to the original decoder.
        if (!cacheHelper.hasLocalCache()) {
            return delegate.decode(response, type)
        }

        val headers = response.headers()

        // Skip the cache flow if the headers lack cache-id or cache-status.
        if (!headers.containsKey(ClientCacheKey.HEADER_KEY_CACHE_UID) ||
            !headers.containsKey(ClientCacheKey.HEADER_KEY_CACHE_STATUS)
        ) {
            return delegate.decode(response, type)
        }

        // Local cache key for this request: url+method+params
        val cacheKeys = response.request().headers()[ClientCacheKey.HEADER_KEY_CACHE_KEY]
        val cacheKey = cacheKeys?.firstOrNull()
        if (cacheKey.isNullOrEmpty()) {
            return delegate.decode(response, type)
        }

        // Read the response cache-status.
        val status = headers[ClientCacheKey.HEADER_KEY_CACHE_STATUS]
        val cacheStatus = status?.firstOrNull()
        // DEBUG, not INFO: this runs on every negotiated response, so at INFO it is two lines and two string
        // concatenations per remote call.
        log.debug("Inter-service cache negotiation cacheKey={0} status={1}", cacheKey, cacheStatus)

        // The actual result.
        val result: Any? = if (ClientCacheKey.STATUS_USE_CACHE == cacheStatus) {
            // Header status is 304: load and return the data from the local cache.
            val cacheItem = cacheHelper.loadFromLocalCache(cacheKey)
                ?: throw DecodeException(
                    response.status(),
                    "Inter-service cache negotiation returned 304 but the local entry for key '$cacheKey' is gone, " +
                        "so there is no body to decode: the provider sent an empty response on the strength of the " +
                        "cache-uid we advertised. The entry was most likely evicted between sending the request and " +
                        "decoding the response (the FEIGN-CACHE region is bounded; see ClientCacheHelper). Retrying " +
                        "will succeed, because without a local entry the next request carries no cache-uid and the " +
                        "provider answers with the full body.",
                    response.request()
                )
            cacheItem.cacheData
        } else {
            // Otherwise, put the fresh data into the local cache.
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

    private val log = LogFactory.getLog(this::class)
}
