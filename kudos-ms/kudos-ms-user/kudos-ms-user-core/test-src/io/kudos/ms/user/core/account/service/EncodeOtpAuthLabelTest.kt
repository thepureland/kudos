package io.kudos.ms.user.core.account.service

import io.kudos.ms.user.core.account.service.impl.UserAccountService
import java.net.URLDecoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure-logic test for UserAccountService.encodeOtpAuthLabel — the otpauth:// label encoding.
 *
 * No DB / Spring needed: the function is a pure companion function.
 *
 * @author K
 * @since 1.0.0
 */
internal class EncodeOtpAuthLabelTest {

    @Test
    fun colonIsPercentEncodedSoTheLabelStaysSingleSegment() {
        val label = UserAccountService.encodeOtpAuthLabel("Kudos", "alice")
        assertEquals("Kudos%3Aalice", label)
        // the separating colon must not appear raw, or the otpauth URI would break
        assertFalse(label.contains(":"))
        assertTrue(label.contains("%3A"))
    }

    @Test
    fun spacesAreEncoded() {
        // URLEncoder encodes space as '+'
        assertEquals("My+App%3Ajohn+doe", UserAccountService.encodeOtpAuthLabel("My App", "john doe"))
    }

    @Test
    fun specialCharsAreEncoded() {
        val label = UserAccountService.encodeOtpAuthLabel("Kudos", "alice@example.com")
        assertEquals("Kudos%3Aalice%40example.com", label)
    }

    @Test
    fun roundTripsBackToIssuerColonAccount() {
        // decoding the label must reconstruct the original "issuer:accountName"
        for ((issuer, account) in listOf(
            "Kudos" to "alice",
            "My App" to "john doe",
            "应用" to "用户名",          // non-ASCII
            "a/b" to "c?d=e&f",         // URL-significant chars
        )) {
            val decoded = URLDecoder.decode(UserAccountService.encodeOtpAuthLabel(issuer, account), Charsets.UTF_8)
            assertEquals("$issuer:$account", decoded)
        }
    }

    @Test
    fun nonAsciiIsUtf8PercentEncoded() {
        val label = UserAccountService.encodeOtpAuthLabel("应用", "u")
        // raw multibyte chars must not leak through; '应' => %E5%BA%94 in UTF-8
        assertTrue(label.contains("%E5%BA%94"))
        assertFalse(label.contains("应"))
    }
}
