package io.kudos.ability.cache.interservice.client.init

/**
 * Cross-service cache client-side configuration.
 *
 * Note that `kudos.ability.cache.interservice.client.decoder-enabled` — which controls whether this module
 * registers its global Feign `Decoder` chain — is deliberately **not** a field here. It gates a `@Bean` through
 * `@ConditionalOnProperty`, and conditions are evaluated before any bean exists, so they can only read the
 * `Environment`; a field on this class could never drive that decision. It used to be declared anyway, which
 * made it look like a working switch: setting it in yml worked (the condition read the same key straight from
 * the environment) while setting it on this bean programmatically silently did nothing. One switch with two
 * apparent sources is worse than one with an obvious source, so the field is gone and the property key stays.
 *
 * @property ttlSeconds Feign local cache TTL in seconds, default 10 minutes.
 * @property includeClients `@FeignClient` names that may take part in cache negotiation. Empty (default)
 *   means every client, preserving the previous behaviour.
 * @property excludeClients `@FeignClient` names excluded from cache negotiation; takes precedence over
 *   [includeClients].
 *
 *   Spring Cloud applies every `RequestInterceptor` bean to **every** Feign client, so by default the
 *   `cache-key` / `cache-uid` headers also go out to third-party APIs — leaking an internal content
 *   fingerprint, and risking rejection by gateways that sign or whitelist headers. List those clients here,
 *   or switch to an allowlist via [includeClients].
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class InterServiceCacheClientProperties {
    var ttlSeconds: Int = 600
    var includeClients: MutableSet<String> = mutableSetOf()
    var excludeClients: MutableSet<String> = mutableSetOf()
}
