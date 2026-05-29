package io.kudos.ability.web.guest.provider

import io.kudos.ability.web.guest.init.properties.GuestProperties
import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Unit tests for the default [GuestAccessUniqueKey] (MD5(UA + IP, cipherKey)).
 *
 * Locks in the two invariants apps actually rely on:
 *  1. Stability: identical UA + IP must reproduce the same hash, otherwise visitor records would
 *     fragment per-request.
 *  2. Sensitivity: changing either UA or IP must change the hash, otherwise the fingerprint is
 *     useless.
 *
 * No assertion on the absolute MD5 value — the kudos `cipherKey` doubles as MD5 salt and could
 * legitimately rotate; the test only cares about the equality/inequality contract.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class GuestAccessUniqueKeyTest {

    private val properties = GuestProperties().apply { cookie.cipherKey = "test-salt" }
    private val uniqueKey = GuestAccessUniqueKey(properties)

    @Test
    fun gen_isStableForIdenticalInput() {
        val first = uniqueKey.gen(buildRequest("UA-A", "10.0.0.1"))
        val second = uniqueKey.gen(buildRequest("UA-A", "10.0.0.1"))
        assertEquals(first, second)
    }

    @Test
    fun gen_changesWhenUserAgentChanges() {
        val withUaA = uniqueKey.gen(buildRequest("UA-A", "10.0.0.1"))
        val withUaB = uniqueKey.gen(buildRequest("UA-B", "10.0.0.1"))
        assertNotEquals(withUaA, withUaB, "different UA must produce a different fingerprint")
    }

    @Test
    fun gen_changesWhenIpChanges() {
        val withIp1 = uniqueKey.gen(buildRequest("UA-A", "10.0.0.1"))
        val withIp2 = uniqueKey.gen(buildRequest("UA-A", "10.0.0.2"))
        assertNotEquals(withIp1, withIp2, "different IP must produce a different fingerprint")
    }

    @Test
    fun gen_changesWhenCipherKeyChanges() {
        // The cipherKey acts as MD5 salt. A deployment that rotates the cipherKey must invalidate
        // the existing hash space — otherwise an attacker who scraped previous hashes still gets
        // matches after rotation.
        val first = uniqueKey.gen(buildRequest("UA-A", "10.0.0.1"))
        properties.cookie.cipherKey = "rotated-salt"
        val second = uniqueKey.gen(buildRequest("UA-A", "10.0.0.1"))
        assertNotEquals(first, second)
    }

    @Test
    fun gen_treatsMissingUserAgentAsEmpty() {
        val withMissingUa = uniqueKey.gen(buildRequest(userAgent = null, "10.0.0.1"))
        val withEmptyUa = uniqueKey.gen(buildRequest("", "10.0.0.1"))
        assertEquals(withMissingUa, withEmptyUa, "no UA header should hash same as empty UA, not blow up")
    }

    private fun buildRequest(userAgent: String?, remoteIp: String): MockHttpServletRequest =
        MockHttpServletRequest().apply {
            remoteAddr = remoteIp
            if (userAgent != null) addHeader("User-Agent", userAgent)
        }
}
