package io.kudos.ability.distributed.client.feign.interceptor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [FeignContextSignature] header-name constants.
 *
 * Coverage: constant values are part of the wire protocol between Feign client and server —
 * pin them so an accidental rename breaks the build instead of breaking cross-service calls.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class FeignContextSignatureTest {

    @Test
    fun headerNames_arePinnedToWireProtocol() {
        assertEquals("X-Kudos-Context-Timestamp", FeignContextSignature.TIMESTAMP_HEADER)
        assertEquals("X-Kudos-Context-Nonce", FeignContextSignature.NONCE_HEADER)
        assertEquals("X-Kudos-Context-Signature", FeignContextSignature.SIGNATURE_HEADER)
    }

    @Test
    fun headerNames_areDistinct() {
        val names = setOf(
            FeignContextSignature.TIMESTAMP_HEADER,
            FeignContextSignature.NONCE_HEADER,
            FeignContextSignature.SIGNATURE_HEADER
        )
        assertEquals(3, names.size)
        assertTrue(names.all { it.startsWith("X-Kudos-Context-") })
    }
}
