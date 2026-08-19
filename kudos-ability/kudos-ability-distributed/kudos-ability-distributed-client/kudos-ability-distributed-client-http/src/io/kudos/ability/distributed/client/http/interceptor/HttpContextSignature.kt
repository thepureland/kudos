package io.kudos.ability.distributed.client.http.interceptor

/**
 * Signature headers used by interface-client context propagation.
 *
 * The provider side (`InternalRpcContextSignatureVerifier` in `kudos-ability-distributed-discovery-nacos`)
 * hard-codes the same three names — the two modules cannot share the constant without creating a
 * dependency cycle, so keep them in sync. The values are unchanged from the OpenFeign era on purpose:
 * renaming them would have been a wire-protocol break for no benefit.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object HttpContextSignature {
    const val TIMESTAMP_HEADER: String = "X-Kudos-Context-Timestamp"
    const val NONCE_HEADER: String = "X-Kudos-Context-Nonce"
    const val SIGNATURE_HEADER: String = "X-Kudos-Context-Signature"
}
