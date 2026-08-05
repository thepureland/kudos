package io.kudos.ms.user.client.account.fallback

import io.kudos.ms.user.client.account.proxy.IUserAccountProxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [UserAccountFallback].
 *
 * Coverage:
 *  - Every read-only override returns its documented safe default (null / empty list / empty map / false).
 *  - Edge inputs: empty strings, Unicode arguments, empty collections, large collections.
 *  - `isUserInOrg` always returns false (fail-closed authorization default).
 *  - Wiring: the fallback implements [IUserAccountProxy].
 *
 * @author K
 * @since 1.0.0
 */
internal class UserAccountFallbackTest {

    private val fallback = UserAccountFallback()

    @Test
    fun instantiates_andImplementsProxy() {
        assertTrue(fallback is IUserAccountProxy)
    }

    @Test
    fun getUserById_returnsNull() {
        assertNull(fallback.getUserById("u1"))
        assertNull(fallback.getUserById(""))
        assertNull(fallback.getUserById("用戶-ünï"))
    }

    @Test
    fun getUsersByIds_returnsEmptyMap() {
        assertTrue(fallback.getUsersByIds(emptyList()).isEmpty())
        assertTrue(fallback.getUsersByIds(listOf("u1")).isEmpty())
        assertTrue(fallback.getUsersByIds(listOf("u1", "u2", "")).isEmpty())
        assertTrue(fallback.getUsersByIds((1..1000).map { "u$it" }).isEmpty())
    }

    @Test
    fun getUserId_returnsNull() {
        assertNull(fallback.getUserId("t1", "alice"))
        assertNull(fallback.getUserId("", ""))
        assertNull(fallback.getUserId("租戶", "用戶ünï"))
    }

    @Test
    fun getUserOrgs_returnsEmptyList() {
        assertTrue(fallback.getUserOrgs("u1").isEmpty())
        assertTrue(fallback.getUserOrgs("").isEmpty())
    }

    @Test
    fun isUserInOrg_alwaysReturnsFalse() {
        assertEquals(false, fallback.isUserInOrg("u1", "o1"))
        assertEquals(false, fallback.isUserInOrg("", ""))
        assertEquals(false, fallback.isUserInOrg("用戶", "組織"))
    }

    @Test
    fun getUserIds_returnsEmptyList() {
        assertTrue(fallback.getUserIds("t1").isEmpty())
        assertTrue(fallback.getUserIds("").isEmpty())
    }
}
