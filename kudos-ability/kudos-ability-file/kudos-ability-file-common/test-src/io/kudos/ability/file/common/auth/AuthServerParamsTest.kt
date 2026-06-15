package io.kudos.ability.file.common.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Tests for the authentication parameter classes [AccessKeyServerParam] and
 * [AccessTokenServerParam].
 *
 * Covers: both constructors of AccessKeyServerParam (including null values and
 * Unicode/empty strings), property mutation, the headerValue property of
 * AccessTokenServerParam, and that both implement the [AuthServerParam] marker.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AuthServerParamsTest {

    @Test
    fun accessKey_defaultConstructorLeavesNulls() {
        val p = AccessKeyServerParam()
        assertNull(p.accessKey)
        assertNull(p.secretKey)
        assertIs<AuthServerParam>(p)
    }

    @Test
    fun accessKey_parameterizedConstructorAssignsValues() {
        val p = AccessKeyServerParam("ak-123", "sk-456")
        assertEquals("ak-123", p.accessKey)
        assertEquals("sk-456", p.secretKey)
    }

    @Test
    fun accessKey_parameterizedConstructorAcceptsNulls() {
        val p = AccessKeyServerParam(null, null)
        assertNull(p.accessKey)
        assertNull(p.secretKey)
    }

    @Test
    fun accessKey_propertiesAreMutable() {
        val p = AccessKeyServerParam()
        p.accessKey = "用戶"
        p.secretKey = ""
        assertEquals("用戶", p.accessKey)
        assertEquals("", p.secretKey)
    }

    @Test
    fun accessToken_headerValueDefaultsToNullAndIsMutable() {
        val p = AccessTokenServerParam()
        assertNull(p.headerValue)
        p.headerValue = "Bearer abc.def.ghi"
        assertEquals("Bearer abc.def.ghi", p.headerValue)
        assertIs<AuthServerParam>(p)
    }

}
