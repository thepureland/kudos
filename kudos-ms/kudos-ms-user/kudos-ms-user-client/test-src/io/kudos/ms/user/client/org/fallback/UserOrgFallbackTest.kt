package io.kudos.ms.user.client.org.fallback

import io.kudos.ms.user.client.org.proxy.IUserOrgProxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [UserOrgFallback].
 *
 * Coverage:
 *  - Every read-only override returns its documented safe default (null / empty list / empty map / false).
 *  - Edge inputs: empty strings, Unicode arguments, empty / single / large collections.
 *  - `isUserInOrg` always returns false (fail-closed authorization default).
 *  - Wiring: the fallback implements [IUserOrgProxy].
 *
 * @author K
 * @since 1.0.0
 */
internal class UserOrgFallbackTest {

    private val fallback = UserOrgFallback()

    @Test
    fun instantiates_andImplementsProxy() {
        assertTrue(fallback is IUserOrgProxy)
    }

    @Test
    fun getOrgById_returnsNull() {
        assertNull(fallback.getOrgById("o1"))
        assertNull(fallback.getOrgById(""))
        assertNull(fallback.getOrgById("組織-ünï"))
    }

    @Test
    fun getOrgsByIds_returnsEmptyMap() {
        assertTrue(fallback.getOrgsByIds(emptyList()).isEmpty())
        assertTrue(fallback.getOrgsByIds(listOf("o1")).isEmpty())
        assertTrue(fallback.getOrgsByIds(listOf("o1", "o2", "")).isEmpty())
        assertTrue(fallback.getOrgsByIds((1..1000).map { "o$it" }).isEmpty())
    }

    @Test
    fun getOrgIds_returnsEmptyList() {
        assertTrue(fallback.getOrgIds("t1").isEmpty())
        assertTrue(fallback.getOrgIds("").isEmpty())
    }

    @Test
    fun getOrgAdmins_returnsEmptyList() {
        assertTrue(fallback.getOrgAdmins("o1").isEmpty())
        assertTrue(fallback.getOrgAdmins("").isEmpty())
    }

    @Test
    fun getOrgUsers_returnsEmptyList() {
        assertTrue(fallback.getOrgUsers("o1").isEmpty())
        assertTrue(fallback.getOrgUsers("").isEmpty())
    }

    @Test
    fun isUserInOrg_alwaysReturnsFalse() {
        assertEquals(false, fallback.isUserInOrg("u1", "o1"))
        assertEquals(false, fallback.isUserInOrg("", ""))
        assertEquals(false, fallback.isUserInOrg("用戶", "組織"))
    }

    @Test
    fun getChildOrgs_returnsEmptyList() {
        assertTrue(fallback.getChildOrgs("o1").isEmpty())
        assertTrue(fallback.getChildOrgs("").isEmpty())
    }

    @Test
    fun getParentOrg_returnsNull() {
        assertNull(fallback.getParentOrg("o1"))
        assertNull(fallback.getParentOrg(""))
    }
}
