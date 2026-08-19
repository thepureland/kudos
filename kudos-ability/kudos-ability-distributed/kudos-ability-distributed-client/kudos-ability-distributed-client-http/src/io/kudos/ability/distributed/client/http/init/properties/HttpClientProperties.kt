package io.kudos.ability.distributed.client.http.init.properties

/**
 * kudos interface-client extension configuration.
 *
 * Bound under `kudos.ability.distributed.client.http`. Deliberately **not** merged with Spring Boot's
 * own `spring.http.serviceclient.*` (base-url / timeouts / group definitions) — those stay in Spring's
 * namespace so the standard `PropertiesRestClientHttpServiceGroupConfigurer` keeps working; this class
 * only carries kudos-specific extensions.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class HttpClientProperties {

    /**
     * HMAC signing secret for context-propagation headers. Empty disables signing, matching the
     * OpenFeign-era behavior (the secret was optional there too).
     *
     * The provider verifies with
     * `kudos.ability.distributed.discovery.nacos.rpc-context-filter.context-signature-secret`,
     * which must hold the same value.
     */
    var contextSignatureSecret: String? = null

}
