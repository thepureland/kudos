package io.kudos.ms.auth.provider.oauth2.secret

import io.kudos.ms.auth.provider.oauth2.init.ExternalLoginProperties
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/** Resolves references without logging their location or value; successful caching is local-only and opt-in. */
@Component
open class ClientSecretResolverRegistry(
    private val resolvers: List<IClientSecretResolver>,
    private val properties: ExternalLoginProperties = ExternalLoginProperties(),
) {
    private val cache = ConcurrentHashMap<String, CachedSecret>()

    open fun resolve(reference: String?): String? {
        val normalized = normalize(reference) ?: return null
        val ttlNanos = cacheTtlNanos()
        if (ttlNanos > 0) {
            cache[normalized]?.let { cached ->
                if (System.nanoTime() < cached.expiresAtNanos) return cached.value
                cache.remove(normalized, cached)
            }
        }
        val resolution = resolveFresh(normalized)
        if (resolution.status != ClientSecretResolutionStatus.RESOLVED) return null
        val value = requireNotNull(resolution.value)
        if (ttlNanos > 0) cache[normalized] = CachedSecret(value, System.nanoTime() + ttlNanos)
        return value
    }

    /** Always probes the backing resolver and returns only non-sensitive status metadata. */
    open fun verify(reference: String?): ClientSecretReferenceCheck {
        val normalized = normalize(reference)
            ?: return ClientSecretReferenceCheck(null, ClientSecretResolutionStatus.NOT_CONFIGURED)
        val resolution = resolveFresh(normalized)
        return ClientSecretReferenceCheck(resolution.scheme, resolution.status)
    }

    /** Clears both registry and resolver-owned state; idempotent and safe for repeated rotation callbacks. */
    open fun invalidate(reference: String?) {
        val normalized = normalize(reference) ?: return
        cache.remove(normalized)
        resolvers.filter { safelySupports(it, normalized) }.forEach { resolver ->
            runCatching { resolver.invalidate(normalized) }
        }
    }

    private fun resolveFresh(reference: String): Resolution {
        val scheme = reference.substringBefore(':').lowercase()
        if (!REFERENCE_PATTERN.matches(reference)) {
            return Resolution(scheme, ClientSecretResolutionStatus.INVALID_REFERENCE)
        }
        val candidates = resolvers.filter { safelySupports(it, reference) }
        if (candidates.isEmpty()) return Resolution(scheme, ClientSecretResolutionStatus.UNSUPPORTED_SCHEME)
        if (candidates.size > 1) return Resolution(scheme, ClientSecretResolutionStatus.AMBIGUOUS_RESOLVER)
        return try {
            val value = candidates.single().resolve(reference)?.takeIf { it.isNotBlank() }
            if (value == null) Resolution(scheme, ClientSecretResolutionStatus.NOT_FOUND)
            else Resolution(scheme, ClientSecretResolutionStatus.RESOLVED, value)
        } catch (e: ClientSecretResolutionException) {
            Resolution(scheme, e.status)
        } catch (_: Exception) {
            Resolution(scheme, ClientSecretResolutionStatus.RESOLVER_ERROR)
        }
    }

    private fun safelySupports(resolver: IClientSecretResolver, reference: String): Boolean =
        runCatching { resolver.supports(reference) }.getOrDefault(false)

    private fun normalize(reference: String?): String? = reference?.trim()?.takeIf { it.isNotBlank() }

    private fun cacheTtlNanos(): Long {
        val seconds = properties.secretCacheTtlSeconds
        require(seconds in 0..MAX_CACHE_TTL_SECONDS) {
            "kudos.ms.auth.external-login.secret-cache-ttl-seconds must be between 0 and $MAX_CACHE_TTL_SECONDS"
        }
        return seconds * NANOS_PER_SECOND
    }

    private data class Resolution(
        val scheme: String,
        val status: ClientSecretResolutionStatus,
        val value: String? = null,
    )

    private data class CachedSecret(val value: String, val expiresAtNanos: Long)

    private companion object {
        const val MAX_CACHE_TTL_SECONDS = 3600L
        const val NANOS_PER_SECOND = 1_000_000_000L
        val REFERENCE_PATTERN = Regex("^[a-z][a-z0-9+.-]{0,31}:[^\\s]+$")
    }
}
