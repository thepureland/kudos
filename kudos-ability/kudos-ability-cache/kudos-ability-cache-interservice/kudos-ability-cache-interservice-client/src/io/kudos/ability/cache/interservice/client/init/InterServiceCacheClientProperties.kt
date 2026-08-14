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
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class InterServiceCacheClientProperties {
    var ttlSeconds: Int = 600
}
