package io.kudos.ability.cache.interservice.client.feign

import feign.RequestInterceptor
import feign.RequestTemplate
import io.kudos.ability.cache.interservice.client.core.ClientCacheHelper
import io.kudos.ability.cache.interservice.common.ClientCacheKey
import io.kudos.context.core.KudosContextHolder
import org.apache.commons.codec.digest.Md5Crypt

/**
 * Feign cache request interceptor.
 *
 * Adds client-side cache request headers to Feign requests to drive the inter-service cache negotiation.
 *
 * Core capabilities:
 * 1. Cache key generation: builds a unique cache key from the request URL, method, and body.
 * 2. Local cache lookup: queries the local cache and reads the UUID of the cached entry.
 * 3. Header injection: sets the cache key and UUID as request headers for the server to inspect.
 *
 * Workflow:
 * 1. Check the local cache: return immediately if the client has no local cache.
 * 2. Build the cache key: produce an MD5-hashed cache key from the request information.
 * 3. Set the cache key header.
 * 4. Look up the local cache.
 * 5. Set the cache UID header: if a cached entry is found, write its UUID into the request header.
 *
 * Cache negotiation:
 * - On receiving the request the server compares the UID in the header with the UID of its data.
 * - If they match, the server returns a 304 and the client uses its local cache.
 * - If they differ, the server returns fresh data along with a new UID and the client updates its cache.
 *
 * Key generation rules:
 * - Includes tenant id, application name, request URL, HTTP method, and request body.
 * - Hashed with MD5 to produce a fixed-length key.
 * - Ensures the same request produces the same key.
 *
 * Caveats:
 * - Headers are only added when the client has a local cache.
 * - The cache key includes tenant information for multi-tenant isolation.
 * - The UUID is used to determine whether the cache is stale.
 */
class FeignCacheRequestInterceptor(
    private val cacheHelper: ClientCacheHelper,
    private val applicationName: String?,
) : RequestInterceptor {

    /**
     * Applies request interception, adding cache-related headers.
     *
     * Adds the cache key and cache UID to the Feign request to drive client-side cache negotiation.
     *
     * Workflow:
     * 1. Check the local cache: return immediately if the client has no local cache.
     * 2. Build the cache key: call genCacheKey to derive a unique identifier for the request.
     * 3. Set the cache key header: write the key into the request header so the server can recognize the request.
     * 4. Look up the local cache.
     * 5. Set the cache UID header: if cached data is found, write its UUID into the request header.
     *
     * Headers:
     * - HEADER_KEY_CACHE_KEY: the cache key, used by the server to identify the request.
     * - HEADER_KEY_CACHE_UID: the cache UUID, used to determine whether the cache is stale.
     *
     * Cache negotiation:
     * - The server compares the UID in the header with the UID of its current data.
     * - On match, returns 304 and the client uses its local cache.
     * - On mismatch, returns fresh data along with a new UID.
     *
     * @param requestTemplate the Feign request template
     */
    override fun apply(requestTemplate: RequestTemplate) {
        if (!cacheHelper.hasLocalCache()) {
            return
        }
        val localCacheKey = genCacheKey(requestTemplate)
        // Build the key value for this request.
        requestTemplate.header(ClientCacheKey.HEADER_KEY_CACHE_KEY, localCacheKey)
        val data = cacheHelper.loadFromLocalCache(localCacheKey)
        if (data != null) {
            // Carry the uid value for this request.
            requestTemplate.header(ClientCacheKey.HEADER_KEY_CACHE_UID, data.uuid)
        }
    }

    /**
     * Builds the local cache key.
     *
     * Produces a unique cache key from the request information for identifying and looking up cached data.
     *
     * Workflow:
     * 1. Read request information: extract URL, HTTP method, and request body.
     * 2. Read the tenant id from KudosContext.
     * 3. Build the key string: concatenate delimiter, tenant id, application name, and request information.
     * 4. MD5 hash: use Md5Crypt.apr1Crypt to derive a fixed-length hash.
     *
     * Key composition:
     * - delimiter + tenant id + delimiter + application name + request information
     * - Request information includes the URL, HTTP method, and request body.
     *
     * Hashing:
     * - MD5 (apr1 variant).
     * - Fixed salt "fCache".
     * - Produces a fixed-length hash.
     *
     * Uniqueness guarantees:
     * - Includes the tenant id for multi-tenant isolation.
     * - Includes the application name to distinguish services.
     * - Includes the request information to distinguish requests.
     * - MD5 ensures key uniqueness and fixed length.
     *
     * Caveats:
     * - The same request produces the same key.
     * - The same request from different tenants produces different keys.
     * - Keys have a fixed length, making them easy to store and compare.
     *
     * @param requestTemplate the Feign request template
     * @return the MD5-hashed cache key
     */
    private fun genCacheKey(requestTemplate: RequestTemplate): String {
        val request = requestTemplate.request()
        val tenantId = KudosContextHolder.get().tenantId ?: ""
        val feignCacheKey = ClientCacheKey(request.url(), request.httpMethod().name, request.body())
        val result = arrayOf(
            ClientCacheKey.FEIGN_CACHE_DELIMITER,
            tenantId,
            ClientCacheKey.FEIGN_CACHE_DELIMITER,
            applicationName,
            feignCacheKey.toString()
        ).joinToString()
        return Md5Crypt.apr1Crypt(result, "fCache")
    }

}
