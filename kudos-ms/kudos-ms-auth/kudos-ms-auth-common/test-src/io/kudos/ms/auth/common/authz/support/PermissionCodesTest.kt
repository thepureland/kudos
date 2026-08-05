package io.kudos.ms.auth.common.authz.support

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


/**
 * junit test for [PermissionCodes].
 *
 * Wildcard semantics decide what "grant the whole module" means, so they are pinned here rather
 * than left to the reader of the matcher: a subtly wider rule than intended is a security bug that
 * no other test would notice.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class PermissionCodesTest {

    @Test
    fun exactMatch() {
        assertTrue(PermissionCodes.matches("sys:user:delete", "sys:user:delete"))
        assertFalse(PermissionCodes.matches("sys:user:delete", "sys:user:read"))
        assertFalse(PermissionCodes.matches("sys:user:delete", "sys:role:delete"))
    }

    @Test
    fun trailingWildcardCoversTheSubtree() {
        assertTrue(PermissionCodes.matches("sys:user:*", "sys:user:delete"))
        assertTrue(PermissionCodes.matches("sys:user:*", "sys:user:read"))
        assertFalse(PermissionCodes.matches("sys:user:*", "sys:role:delete"))

        // A shorter grant with a trailing wildcard reaches arbitrarily deep — that is the point of
        // "grant the entire module" being one row instead of an enumeration that goes stale.
        assertTrue(PermissionCodes.matches("sys:*", "sys:user"))
        assertTrue(PermissionCodes.matches("sys:*", "sys:user:delete"))
        assertTrue(PermissionCodes.matches("sys:*", "sys:user:field:read"))
        assertFalse(PermissionCodes.matches("sys:*", "msg:user:delete"))

        assertTrue(PermissionCodes.matches("*", "anything:at:all"))
    }

    @Test
    fun middleWildcardMatchesExactlyOneSegment() {
        assertTrue(PermissionCodes.matches("sys:*:read", "sys:user:read"))
        assertTrue(PermissionCodes.matches("sys:*:read", "sys:role:read"))
        assertFalse(PermissionCodes.matches("sys:*:read", "sys:user:delete"))
        // Must not swallow a deeper code: the wildcard is one segment, not a subtree.
        assertFalse(PermissionCodes.matches("sys:*:read", "sys:user:field:read"))
    }

    @Test
    fun grantLongerThanRequestNeverMatches() {
        assertFalse(PermissionCodes.matches("sys:user:delete", "sys:user"))
        assertFalse(PermissionCodes.matches("sys:user:*", "sys:user"))
    }

    @Test
    fun requestWildcardsAreLiteral() {
        // "may I do anything under sys" is not a question the decision point answers; a request is
        // always concrete, so a `*` in it is just a segment.
        assertFalse(PermissionCodes.matches("sys:user:delete", "sys:user:*"))
        assertTrue(PermissionCodes.matches("sys:user:*", "sys:user:*"))
    }

    @Test
    fun blankNeverMatches() {
        assertFalse(PermissionCodes.matches(null, "sys:user:delete"))
        assertFalse(PermissionCodes.matches("", "sys:user:delete"))
        assertFalse(PermissionCodes.matches("sys:user:delete", null))
        assertFalse(PermissionCodes.matches("sys:user:delete", "  "))
    }

    @Test
    fun anyMatches() {
        val held = listOf("msg:template:read", "sys:user:*")
        assertTrue(PermissionCodes.anyMatches(held, "sys:user:delete"))
        assertFalse(PermissionCodes.anyMatches(held, "sys:role:delete"))
        assertFalse(PermissionCodes.anyMatches(emptyList(), "sys:user:delete"))
    }
}
