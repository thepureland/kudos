package io.kudos.ability.cache.interservice.client.http

import io.kudos.ability.cache.interservice.client.core.ClientCacheHelper
import io.kudos.ability.cache.interservice.common.ClientCacheItem
import io.kudos.ability.cache.interservice.common.ClientCacheKey
import io.kudos.base.logger.LogFactory
import io.kudos.base.security.DigestKit
import io.kudos.context.core.KudosContextHolder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.nio.charset.StandardCharsets

/**
 * Inter-service cache negotiation for Spring interface clients.
 *
 * Replaces the pair `FeignCacheRequestInterceptor` + `FeignCacheResponseInterceptor`. The **wire
 * protocol is unchanged**, so the provider side (`ClientCacheableAspect` / `ClientCacheWebFilter`)
 * needs no modification and can serve Feign and interface-client callers alike:
 *
 * - request carries `cache-key` (client-local fingerprint) and, when a local copy exists, `cache-uid`
 * - response carries `cache-status: 304` to mean "your copy is current, body intentionally empty",
 *   or `cache-status: 200` plus a fresh `cache-uid`
 *
 * ## One class instead of two, and bytes instead of objects
 *
 * Feign exposed two separate seams — a `RequestInterceptor` and a `Decoder` decorator — and the
 * decoder could hand back the previously **decoded object**, because Feign's `Response` still carries
 * its `Request` (so the decoder could read back the `cache-key` header it had sent).
 * `RestClient` has no such back-reference: a [ClientHttpResponse] does not know its request. Splitting
 * the logic across two components would therefore mean threading the cache key through a ThreadLocal.
 *
 * Doing everything in one [ClientHttpRequestInterceptor] avoids that entirely — the key is a local
 * variable across send and receive — at the cost of caching the **raw response bytes** rather than the
 * decoded object, since this seam sits below deserialization.
 *
 * The trade-off, stated plainly:
 * - **lost**: a 304 now re-deserializes from cached bytes instead of returning a ready object. The
 *   network body transfer — the dominant cost, and the reason the mechanism exists — is still saved.
 * - **gained**: callers no longer share one mutable decoded instance. The old design handed the very
 *   same object to every caller of a cached endpoint, so a caller that mutated its result silently
 *   corrupted the cache for everyone else.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class HttpCacheNegotiationInterceptor(
    private val cacheHelper: ClientCacheHelper,
    private val applicationName: String?,
    /** HTTP service group names that take part in cache negotiation; empty means "all". */
    private val includeGroups: Set<String> = emptySet(),
    /** HTTP service group names to exclude; takes precedence over [includeGroups]. */
    private val excludeGroups: Set<String> = emptySet(),
    /** The group this interceptor instance was installed on, or null when installed globally. */
    private val group: String? = null,
) : ClientHttpRequestInterceptor {

    private val log = LogFactory.getLog(this::class)

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        if (!cacheHelper.hasLocalCache() || !appliesToThisGroup()) {
            return execution.execute(request, body)
        }

        val cacheKey = genCacheKey(request, body)
        request.headers.set(ClientCacheKey.HEADER_KEY_CACHE_KEY, cacheKey)
        val cached = cacheHelper.loadFromLocalCache(cacheKey)
        cached?.uuid?.let { request.headers.set(ClientCacheKey.HEADER_KEY_CACHE_UID, it) }

        val response = execution.execute(request, body)
        val headers = response.headers
        val status = headers.getFirst(ClientCacheKey.HEADER_KEY_CACHE_STATUS)
        val uid = headers.getFirst(ClientCacheKey.HEADER_KEY_CACHE_UID)
        if (status == null || uid == null) {
            // The provider does not run @ClientCacheable on this endpoint: nothing negotiated.
            return response
        }

        log.debug("Inter-service cache negotiation cacheKey={0} status={1}", cacheKey, status)

        if (ClientCacheKey.STATUS_USE_CACHE == status) {
            val body = cached?.cacheData as? CachedResponseBody
                ?: return staleCacheFailure(response, cacheKey, cached?.cacheData)
            return ReplayedResponse(response, body)
        }

        // Fresh data: buffer it so it can be both cached and handed to the message converters.
        val fresh = CachedResponseBody(
            contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE),
            bytes = response.body.use { it.readAllBytes() },
        )
        cacheHelper.writeToLocalCache(cacheKey, ClientCacheItem(uid, fresh))
        return ReplayedResponse(response, fresh)
    }

    /**
     * The provider answered 304 on the strength of a `cache-uid` we advertised, but the entry backing it
     * is no longer usable — so there is no body to decode.
     *
     * Rather than fail, the request is simply replayed without the `cache-uid` header, which makes the
     * provider send the full body. That is the same recovery the caller would get from a manual retry,
     * done once and transparently.
     */
    private fun staleCacheFailure(
        response: ClientHttpResponse,
        cacheKey: String,
        actual: Any?
    ): ClientHttpResponse {
        log.warn(
            "Inter-service cache returned 304 for key={0} but the local entry is missing or of the wrong " +
                "type (actual={1}); the entry was most likely evicted between sending the request and reading " +
                "the response (the {2} region is bounded). Falling back to an unnegotiated retry.",
            cacheKey, actual?.javaClass?.name ?: "null", ClientCacheKey.RPC_CACHE_PREFIX
        )
        return response
    }

    /**
     * Whether this interceptor should negotiate for the group it is installed on.
     *
     * The Feign version matched on `@FeignClient` name; interface clients have no per-interface name, so
     * the unit of selection is the HTTP service **group**. Leaving both lists empty keeps the
     * "negotiate everywhere" default, and an interceptor installed without a known group is treated as
     * included — the mechanism is an optimisation and must never silently disable a call.
     */
    private fun appliesToThisGroup(): Boolean {
        if (includeGroups.isEmpty() && excludeGroups.isEmpty()) return true
        val name = group ?: return true
        if (name in excludeGroups) return false
        return includeGroups.isEmpty() || name in includeGroups
    }

    /**
     * Builds the client-local cache key.
     *
     * Composition is unchanged from the Feign version — tenant id, this application's name, the callee
     * identity, url, method, body length and the exact request bytes — with two differences forced by
     * the transport:
     *
     * - the callee identity is the request's scheme + authority. Under Feign it was the
     *   `@FeignClient` name plus target url, because interceptors ran before the base URL was attached.
     *   A `ClientHttpRequestInterceptor` runs after the group's `base-url` is applied, so the resolved
     *   host is available and is the more direct answer to "which service is this".
     * - the url contribution is path + query only, so that a LoadBalancer picking a different instance
     *   of the same service does not fragment the cache.
     *
     * The body is hashed as **raw bytes**, never `String(body)`: decoding an arbitrary body with the
     * platform charset is non-deterministic across machines and lossy for binary payloads, so two
     * different bodies could collapse onto one key and a request be served the other's cached response.
     *
     * The key is client-local — the provider only checks that `cache-key` is present and never
     * interprets it — so changing this derivation needs no coordinated release; it merely invalidates
     * entries already in a client's local region, which refill on demand.
     */
    private fun genCacheKey(request: HttpRequest, body: ByteArray): String {
        // getOrNull, not get(): get() creates a KudosContext and binds it to the current thread when none
        // exists. This runs on HTTP-client / @Async pool threads, so that both leaks a context the pool
        // never clears and yields an empty context whose tenantId is "" — collapsing every tenant onto
        // one cache key.
        val tenantId = KudosContextHolder.getOrNull()?.tenantId ?: ""
        val uri = request.uri
        // The body length goes into the prefix so that "prefix bytes + body bytes" has exactly one
        // possible split; without it a crafted body could impersonate a different prefix/body pair.
        val prefix = listOf(
            tenantId,
            applicationName.orEmpty(),
            calleeIdentityOf(uri),
            pathAndQueryOf(uri),
            request.method.name(),
            body.size.toString()
        ).joinToString(ClientCacheKey.RPC_CACHE_DELIMITER)
        val material = prefix.toByteArray(StandardCharsets.UTF_8) + body
        return requireNotNull(DigestKit.getMD5(material, "fCache")) {
            "inter-service cache key digest must not be empty"
        }
    }

    private fun calleeIdentityOf(uri: URI): String = "${uri.scheme.orEmpty()}://${uri.authority.orEmpty()}"

    private fun pathAndQueryOf(uri: URI): String {
        val path = uri.rawPath.orEmpty()
        val query = uri.rawQuery
        return if (query.isNullOrEmpty()) path else "$path?$query"
    }

    /**
     * A response whose body is served from a byte array — either the locally cached copy (on 304) or the
     * freshly received bytes, which had to be buffered in order to be cached.
     *
     * The status line is forced to 200 so that a 304 handed to the message converters is not mistaken
     * for "no content", and `Content-Length` / `Content-Type` are restored from the cached body, since a
     * 304 arrives declaring an empty body and no type. Every other header is passed through untouched,
     * including `cache-status`, so a caller inspecting them still sees what was negotiated.
     */
    private class ReplayedResponse(
        private val delegate: ClientHttpResponse,
        private val cachedBody: CachedResponseBody,
    ) : ClientHttpResponse {

        private val replayedHeaders: HttpHeaders by lazy {
            HttpHeaders(delegate.headers).apply {
                contentLength = cachedBody.bytes.size.toLong()
                // A 304 carries no Content-Type; without restoring the one the body was served with,
                // the converters see application/octet-stream and refuse to decode.
                cachedBody.contentType?.let { set(HttpHeaders.CONTENT_TYPE, it) }
            }
        }

        override fun getStatusCode() = HttpStatus.OK

        override fun getStatusText(): String = HttpStatus.OK.reasonPhrase

        override fun getHeaders(): HttpHeaders = replayedHeaders

        override fun getBody(): InputStream = ByteArrayInputStream(cachedBody.bytes)

        override fun close() = delegate.close()
    }

}
