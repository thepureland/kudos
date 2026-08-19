package io.kudos.ability.cache.interservice.client.init

/**
 * Client-side inter-service cache configuration.
 *
 * Bound under `kudos.ability.cache.interservice.client`.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
class InterServiceCacheClientProperties {

    /** TTL of the local negotiation cache region, in seconds. Must be greater than 0. */
    var ttlSeconds: Int = 600

    /**
     * HTTP service group names that take part in cache negotiation; empty means "all".
     *
     * Replaces the Feign-era `includeClients`, which matched `@FeignClient` names. Interface clients
     * have no per-interface name — the unit of selection is the group declared by
     * `@ImportHttpServices(group = ...)`, which is also the key under `spring.http.serviceclient`.
     */
    var includeGroups: MutableSet<String> = mutableSetOf()

    /** HTTP service group names to exclude; takes precedence over [includeGroups]. */
    var excludeGroups: MutableSet<String> = mutableSetOf()

}
