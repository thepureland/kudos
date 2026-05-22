package io.kudos.ability.distributed.client.feign.init.properties

/**
 * kudos OpenFeign 扩展配置。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class OpenFeignProperties {

    /**
     * 上下文透传头 HMAC 签名密钥。为空时不签名，保持历史行为。
     */
    var contextSignatureSecret: String? = null

}
