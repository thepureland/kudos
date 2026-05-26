package io.kudos.ability.cache.interservice.provider.init

/**
 * Cross-service cache provider-side configuration.
 *
 * @property uidCacheEnabled Whether to cache the response-object-to-UID mapping. Disabled by
 *   default to avoid reusing a stale UID for mutable objects.
 * @property wrapAllRequests Whether the filter wraps every request. By default only requests that
 *   carry cross-service cache headers are wrapped.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class InterServiceCacheProviderProperties {
    var uidCacheEnabled: Boolean = false
    var wrapAllRequests: Boolean = false
}
