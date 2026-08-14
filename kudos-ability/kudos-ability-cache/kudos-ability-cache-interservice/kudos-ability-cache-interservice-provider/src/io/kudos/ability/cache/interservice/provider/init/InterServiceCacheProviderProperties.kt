package io.kudos.ability.cache.interservice.provider.init

/**
 * Cross-service cache provider-side configuration.
 *
 * @property uidCacheEnabled Whether to cache the response-object-to-UID mapping. Disabled by
 *   default to avoid reusing a stale UID for mutable objects.
 * @property wrapAllRequests Whether the filter wraps every request. By default only requests that
 *   carry cross-service cache headers are wrapped.
 * @property requireFeignMarker Whether to additionally require the internal Feign marker header
 *   ([io.kudos.context.support.Consts.RequestHeader.FEIGN_REQUEST]) before a request may take part in cache
 *   negotiation.
 *
 *   Entering negotiation is decided purely by the presence of the `cache-key` / `cache-uid` headers, so any
 *   caller that sets them takes part — including callers from outside the service mesh. That lets an external
 *   caller read a stable MD5 fingerprint of the response off the `cache-uid` response header, and make the
 *   provider answer with an empty body by sending a matching one.
 *
 *   Practical exposure is limited: obtaining the fingerprint requires being able to invoke the endpoint, and
 *   anyone who can invoke it can simply read the body. It is worth closing on services that are reachable from
 *   outside, which is what this flag does.
 *
 *   **Defaults to `false`, i.e. current behaviour.** Turning it on where the marker is not actually sent would
 *   silently disable the whole provider side, and the marker comes from `GlobalHeaderRequestInterceptor` in
 *   `kudos-ability-distributed-client-feign` — a module this one depends on only at compile/test time, so its
 *   presence at runtime is not guaranteed. Enable it once you have confirmed every caller of this service goes
 *   through that interceptor. (Contrast `nacos`'s `allowUnmarkedContextHeaders`, which defaults to the strict
 *   side because its strict path merely skips populating context rather than switching a feature off.)
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class InterServiceCacheProviderProperties {
    var uidCacheEnabled: Boolean = false
    var wrapAllRequests: Boolean = false
    var requireFeignMarker: Boolean = false
}
