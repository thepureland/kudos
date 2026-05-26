package io.kudos.ability.distributed.client.feign.init.properties

/**
 * kudos OpenFeign extension configuration.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class OpenFeignProperties {

    /**
     * HMAC signing secret for context-propagation headers. Empty disables signing, preserving legacy behavior.
     */
    var contextSignatureSecret: String? = null

}
