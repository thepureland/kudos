package io.kudos.ability.distributed.discovery.nacos.filter

import io.kudos.context.support.Consts
import jakarta.servlet.http.HttpServletRequest
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

/**
 * Provider-side verifier for the HMAC context-propagation signature produced by
 * `GlobalHeaderRequestInterceptor` (module `kudos-ability-distributed-client-feign`).
 *
 * The client signs the payload below with HMAC-SHA256 (Base64-encoded) when
 * `kudos.ability.distributed.client.feign.contextSignatureSecret` is configured:
 *
 * ```
 * method \n url \n TENANT_ID \n SUB_SYS_CODE \n TRACE_KEY \n DATASOURCE_ID \n LOCAL \n timestamp \n nonce
 * ```
 *
 * where `url` is the Feign template URL at interceptor time (path plus query string, before the
 * target host is applied) and missing headers contribute empty strings. This verifier rebuilds the
 * same payload from the inbound [HttpServletRequest] and checks:
 *
 *  1. **Signature headers present** — timestamp, nonce and signature must all be non-blank.
 *  2. **Timestamp window** — the signed timestamp must be within plus or minus
 *     [timestampWindowMillis] of the provider clock (default 5 minutes), limiting replay exposure
 *     and tolerating moderate clock skew.
 *  3. **HMAC signature** — recomputed over the inbound method, URL and context headers and compared
 *     with [MessageDigest.isEqual] (constant-time) to avoid timing side channels.
 *  4. **Nonce replay** — each nonce is accepted once within its validity window. Seen nonces are
 *     kept in a bounded in-process map whose entries expire at `timestamp + window` (the exact
 *     moment the timestamp check would reject a replay anyway), so the TTL matches the timestamp
 *     window. The map is capped at [nonceCacheMaxSize] entries (oldest evicted first) to prevent
 *     unbounded memory growth.
 *
 * URL matching: because gateways or servlet context paths may prefix the path the client signed,
 * the verifier accepts a match against either the full `requestURI` or the `requestURI` with the
 * servlet context path stripped (query string appended in both cases). Percent-encoding must be
 * identical on both sides — re-encoding proxies are not supported.
 *
 * TODO: the nonce store is in-process only. In a multi-instance deployment a replayed request can
 * be accepted once per instance within the timestamp window; switch the store to a shared cache
 * (e.g. Redis `SET key NX PX window`) for cluster-wide replay protection.
 *
 * Header names are mirrored from `FeignContextSignature` in the client module — the two modules
 * cannot share the constant without creating a dependency cycle, so keep them in sync.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class FeignContextSignatureVerifier(
    secret: String,
    private val timestampWindowMillis: Long = DEFAULT_TIMESTAMP_WINDOW_MILLIS,
    private val nonceCacheMaxSize: Int = DEFAULT_NONCE_CACHE_MAX_SIZE,
    private val clock: Clock = Clock.systemUTC()
) {

    init {
        require(secret.isNotBlank()) { "secret must not be blank" }
        require(timestampWindowMillis > 0) { "timestampWindowMillis must be positive" }
        require(nonceCacheMaxSize > 0) { "nonceCacheMaxSize must be positive" }
    }

    private val secretBytes = secret.toByteArray(StandardCharsets.UTF_8)

    /**
     * Seen-nonce map: nonce -> expiry epoch millis. Insertion-ordered with a hard size cap so a
     * burst of unique nonces cannot exhaust memory; expired head entries are purged on each insert.
     * All access is guarded by `synchronized(seenNonces)`.
     */
    private val seenNonces = object : LinkedHashMap<String, Long>(16, 0.75f) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>): Boolean =
            size > nonceCacheMaxSize
    }

    /**
     * Verification outcome. Anything other than [OK] should cause the request to be rejected.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    enum class Result {
        OK,
        MISSING_SIGNATURE_HEADERS,
        INVALID_TIMESTAMP,
        TIMESTAMP_OUT_OF_WINDOW,
        SIGNATURE_MISMATCH,
        NONCE_REPLAYED
    }

    /**
     * Verify the context-propagation signature of the given request.
     *
     * The nonce is only recorded after the signature check succeeds, so unauthenticated requests
     * cannot poison the nonce cache.
     *
     * @param request inbound HTTP request carrying the context and signature headers
     * @return [Result.OK] when the request is authentic and fresh, otherwise the failure reason
     */
    fun verify(request: HttpServletRequest): Result {
        val timestamp = request.getHeader(TIMESTAMP_HEADER)
        val nonce = request.getHeader(NONCE_HEADER)
        val signature = request.getHeader(SIGNATURE_HEADER)
        if (timestamp.isNullOrBlank() || nonce.isNullOrBlank() || signature.isNullOrBlank()) {
            return Result.MISSING_SIGNATURE_HEADERS
        }
        val timestampMillis = timestamp.toLongOrNull() ?: return Result.INVALID_TIMESTAMP
        val now = clock.millis()
        if (abs(now - timestampMillis) > timestampWindowMillis) {
            return Result.TIMESTAMP_OUT_OF_WINDOW
        }
        if (!signatureMatches(request, timestamp, nonce, signature)) {
            return Result.SIGNATURE_MISMATCH
        }
        if (!registerNonce(nonce, now, timestampMillis + timestampWindowMillis)) {
            return Result.NONCE_REPLAYED
        }
        return Result.OK
    }

    private fun signatureMatches(
        request: HttpServletRequest,
        timestamp: String,
        nonce: String,
        providedSignature: String
    ): Boolean {
        val provided = providedSignature.toByteArray(StandardCharsets.UTF_8)
        return candidateUrls(request).any { url ->
            val expected = hmacSha256(signaturePayload(request, url, timestamp, nonce))
            // Constant-time comparison: never short-circuit on the first differing byte.
            MessageDigest.isEqual(expected.toByteArray(StandardCharsets.UTF_8), provided)
        }
    }

    /**
     * Candidate URLs the client may have signed: the raw requestURI, plus the requestURI with the
     * servlet context path stripped (clients usually sign the path relative to the service root).
     */
    private fun candidateUrls(request: HttpServletRequest): List<String> {
        val requestUri = request.requestURI.orEmpty()
        val paths = LinkedHashSet<String>()
        paths += requestUri
        val contextPath = request.contextPath.orEmpty()
        if (contextPath.isNotEmpty() && requestUri.startsWith(contextPath)) {
            paths += requestUri.substring(contextPath.length)
        }
        val query = request.queryString
        return paths.map { if (query.isNullOrEmpty()) it else "$it?$query" }
    }

    /**
     * Mirror of the client-side payload built by `GlobalHeaderRequestInterceptor.signaturePayload`.
     */
    private fun signaturePayload(
        request: HttpServletRequest,
        url: String,
        timestamp: String,
        nonce: String
    ): String = listOf(
        request.method.orEmpty(),
        url,
        request.getHeader(Consts.RequestHeader.TENANT_ID).orEmpty(),
        request.getHeader(Consts.RequestHeader.SUB_SYS_CODE).orEmpty(),
        request.getHeader(Consts.RequestHeader.TRACE_KEY).orEmpty(),
        request.getHeader(Consts.RequestHeader.DATASOURCE_ID).orEmpty(),
        request.getHeader(Consts.RequestHeader.LOCAL).orEmpty(),
        timestamp,
        nonce
    ).joinToString("\n")

    private fun hmacSha256(payload: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(secretBytes, HMAC_ALGORITHM))
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8)))
    }

    /**
     * Record the nonce; returns false when it was already seen (replay).
     */
    private fun registerNonce(nonce: String, nowMillis: Long, expiresAtMillis: Long): Boolean {
        synchronized(seenNonces) {
            val iterator = seenNonces.entries.iterator()
            while (iterator.hasNext()) {
                // Purge expired entries from the (oldest-first) head; stop at the first live one.
                if (iterator.next().value <= nowMillis) iterator.remove() else break
            }
            if (seenNonces.containsKey(nonce)) {
                return false
            }
            seenNonces[nonce] = expiresAtMillis
            return true
        }
    }

    companion object {
        /** Must stay in sync with `FeignContextSignature.TIMESTAMP_HEADER` in client-feign. */
        const val TIMESTAMP_HEADER: String = "X-Kudos-Context-Timestamp"

        /** Must stay in sync with `FeignContextSignature.NONCE_HEADER` in client-feign. */
        const val NONCE_HEADER: String = "X-Kudos-Context-Nonce"

        /** Must stay in sync with `FeignContextSignature.SIGNATURE_HEADER` in client-feign. */
        const val SIGNATURE_HEADER: String = "X-Kudos-Context-Signature"

        /** Default timestamp acceptance window: plus or minus 5 minutes. */
        const val DEFAULT_TIMESTAMP_WINDOW_MILLIS: Long = 5 * 60 * 1000L

        /** Default upper bound of the in-process seen-nonce map. */
        const val DEFAULT_NONCE_CACHE_MAX_SIZE: Int = 100_000

        private const val HMAC_ALGORITHM = "HmacSHA256"
    }
}
