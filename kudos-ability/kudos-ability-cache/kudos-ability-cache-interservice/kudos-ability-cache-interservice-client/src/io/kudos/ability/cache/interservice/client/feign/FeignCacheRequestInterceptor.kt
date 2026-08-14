package io.kudos.ability.cache.interservice.client.feign

import feign.RequestInterceptor
import feign.RequestTemplate
import io.kudos.ability.cache.interservice.client.core.ClientCacheHelper
import io.kudos.ability.cache.interservice.common.ClientCacheKey
import io.kudos.base.security.DigestKit
import io.kudos.context.core.KudosContextHolder
import java.nio.charset.StandardCharsets

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
    /** Feign client names to participate in cache negotiation; empty means "all". */
    private val includeClients: Set<String> = emptySet(),
    /** Feign client names to exclude; takes precedence over [includeClients]. */
    private val excludeClients: Set<String> = emptySet(),
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
        if (!appliesTo(requestTemplate)) {
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
     * 4. Digest: a single salted MD5 pass over the key material, producing a fixed-length hash.
     *
     * Key composition:
     * - delimiter + tenant id + delimiter + application name + request information
     * - Request information includes the URL, HTTP method, and request body.
     *
     * Hashing:
     * - A single MD5 pass over the key material, salted with "fCache".
     * - This is a fingerprint, not a credential: MD5's dispersion is what matters here, not its strength.
     *
     * It used to call `Md5Crypt.apr1Crypt`, which is the opposite of what a hot path wants — apr1 is a
     * *deliberately slow* password hash that runs 1000 MD5 rounds, and every round re-hashes plaintext that
     * includes the **entire request body**. A 1 MB POST therefore cost on the order of a gigabyte of hashing,
     * once per Feign call.
     *
     * The body is hashed as **raw bytes** rather than via `String(body)`. Decoding an arbitrary body with the
     * platform charset is both non-deterministic across machines and lossy for binary payloads (protobuf, file
     * uploads): unmappable bytes all collapse to the replacement character, so two different bodies could
     * produce the same key and one request could be served the other's cached response.
     *
     * Uniqueness guarantees:
     * - Includes the tenant id for multi-tenant isolation.
     * - Includes the callee identity ([targetIdentityOf]) so two services sharing a path do not share a key.
     * - Includes url, method and the exact request bytes.
     *
     * Note that the key is **client-local**: the provider only checks that the `cache-key` header is present
     * and never interprets its value, so changing this derivation needs no coordinated client/provider release.
     * It does invalidate every entry already in a client's local region, which then refills on demand.
     *
     * @param requestTemplate the Feign request template
     * @return the MD5-hashed cache key
     */
    private fun genCacheKey(requestTemplate: RequestTemplate): String {
        val request = requestTemplate.request()
        // getOrNull, not get(): get() creates a KudosContext and binds it to the current thread when none
        // exists. Feign runs on HTTP-client / @Async / bulkhead pool threads, so that both leaks a context the
        // pool never clears (the holder's own KDoc warns about exactly this) and yields an *empty* context whose
        // tenantId is "" — which would collapse every tenant onto one cache key.
        val tenantId = KudosContextHolder.getOrNull()?.tenantId ?: ""
        val body = request.body() ?: EMPTY_BODY
        // The body length goes into the prefix so that "prefix bytes + body bytes" has exactly one possible
        // split; without it a crafted body could impersonate a different prefix/body pair.
        val prefix = listOf(
            tenantId,
            applicationName.orEmpty(),
            targetIdentityOf(requestTemplate),
            request.url(),
            request.httpMethod().name,
            body.size.toString()
        ).joinToString(ClientCacheKey.FEIGN_CACHE_DELIMITER)
        val material = prefix.toByteArray(StandardCharsets.UTF_8) + body
        return requireNotNull(DigestKit.getMD5(material, "fCache")) { "feign cache key digest must not be empty" }
    }

    /**
     * Identifies the service being called.
     *
     * Without this the key is ambiguous across services. `RequestInterceptor`s run *before*
     * `Target.apply(template)` attaches the base URL, so at this point `request.url()` is only the path; and
     * `applicationName` is this application's own name — the same value for every Feign client in the JVM.
     * So `serviceA.get("/config")` and `serviceB.get("/config")` produced identical cache keys: one service's
     * response would overwrite the other's, and a 304 for one would be answered from the other's data.
     *
     * Feign sets the target on the template when it is created, so it is already available here; it is read
     * defensively all the same, since the whole mechanism is optional and must not break the call if a Feign
     * version stops populating it.
     */
    /**
     * Whether this Feign client should take part in cache negotiation.
     *
     * Spring Cloud applies every `RequestInterceptor` bean to **every** `@FeignClient`, so without this the
     * `cache-key` / `cache-uid` headers were attached to calls to third-party APIs as well — leaking an
     * internal content fingerprint to them, and risking rejection by gateways that sign or whitelist headers.
     *
     * Matching is by the `@FeignClient` name. `excludeClients` wins over `includeClients`; leaving both empty
     * keeps the previous "apply everywhere" behaviour, so nothing changes until a list is configured. A client
     * whose target cannot be resolved is treated as included, for the same reason: never silently disable.
     */
    private fun appliesTo(requestTemplate: RequestTemplate): Boolean {
        if (includeClients.isEmpty() && excludeClients.isEmpty()) return true
        val name = runCatching { requestTemplate.feignTarget()?.name() }.getOrNull() ?: return true
        if (name in excludeClients) return false
        return includeClients.isEmpty() || name in includeClients
    }

    private fun targetIdentityOf(requestTemplate: RequestTemplate): String {
        val target = runCatching { requestTemplate.feignTarget() }.getOrNull() ?: return ""
        return "${target.name()}|${target.url()}"
    }

    private companion object {
        /** Shared stand-in for "no request body", so GET requests do not allocate one per call. */
        private val EMPTY_BODY = ByteArray(0)
    }

}
