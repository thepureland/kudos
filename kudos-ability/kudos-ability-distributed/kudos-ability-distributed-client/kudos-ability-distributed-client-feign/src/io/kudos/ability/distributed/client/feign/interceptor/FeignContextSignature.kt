package io.kudos.ability.distributed.client.feign.interceptor

/**
 * Signature headers used by Feign context propagation.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object FeignContextSignature {
    const val TIMESTAMP_HEADER: String = "X-Kudos-Context-Timestamp"
    const val NONCE_HEADER: String = "X-Kudos-Context-Nonce"
    const val SIGNATURE_HEADER: String = "X-Kudos-Context-Signature"
}
