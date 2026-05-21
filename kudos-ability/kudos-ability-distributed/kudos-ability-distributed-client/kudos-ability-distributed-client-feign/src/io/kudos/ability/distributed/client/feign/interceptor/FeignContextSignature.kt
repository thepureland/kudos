package io.kudos.ability.distributed.client.feign.interceptor

/**
 * Feign 上下文透传签名头。
 */
object FeignContextSignature {
    const val TIMESTAMP_HEADER: String = "X-Kudos-Context-Timestamp"
    const val NONCE_HEADER: String = "X-Kudos-Context-Nonce"
    const val SIGNATURE_HEADER: String = "X-Kudos-Context-Signature"
}
