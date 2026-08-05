package io.kudos.ms.user.common.passport

import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * test for CurrentUserKit
 *
 * Pure unit test that drives [KudosContextHolder] directly (no DB / no Spring). Covers:
 *  - no context bound at all (getOrNull() == null path)
 *  - context bound but user == null
 *  - context bound with a non-[SessionUserPrincipal] user (the `as?` safe-cast null path)
 *  - context bound with a real [SessionUserPrincipal] (happy path for every accessor)
 *  - currentUserId() throwing IllegalStateException with the documented message
 *
 * @author K
 * @since 1.0.0
 */
internal class CurrentUserKitTest {

    @BeforeTest
    fun clearBefore() = KudosContextHolder.clear()

    @AfterTest
    fun clearAfter() = KudosContextHolder.clear()

    private fun bindUser(user: IIdEntity<String>?) {
        val ctx = KudosContext()
        ctx.user = user
        KudosContextHolder.set(ctx)
    }

    @Test
    fun noContextBound_returnsNull() {
        // getOrNull() returns null -> every accessor null
        assertNull(CurrentUserKit.currentPrincipalOrNull())
        assertNull(CurrentUserKit.currentUserIdOrNull())
        assertNull(CurrentUserKit.currentTenantIdOrNull())
    }

    @Test
    fun contextBoundButUserNull_returnsNull() {
        bindUser(null)
        assertNull(CurrentUserKit.currentPrincipalOrNull())
        assertNull(CurrentUserKit.currentUserIdOrNull())
        assertNull(CurrentUserKit.currentTenantIdOrNull())
    }

    @Test
    fun contextBoundWithNonPrincipalUser_safeCastReturnsNull() {
        // user is an IIdEntity but NOT a SessionUserPrincipal -> `as?` yields null
        bindUser(object : IIdEntity<String> {
            override val id: String get() = "other"
        })
        assertNull(CurrentUserKit.currentPrincipalOrNull())
        assertNull(CurrentUserKit.currentUserIdOrNull())
        assertNull(CurrentUserKit.currentTenantIdOrNull())
    }

    @Test
    fun happyPath_readsAllFields() {
        val principal = SessionUserPrincipal(id = "u-1", tenantId = "t-1", username = "alice")
        bindUser(principal)
        assertEquals(principal, CurrentUserKit.currentPrincipalOrNull())
        assertEquals("u-1", CurrentUserKit.currentUserIdOrNull())
        assertEquals("u-1", CurrentUserKit.currentUserId())
        assertEquals("t-1", CurrentUserKit.currentTenantIdOrNull())
    }

    @Test
    fun currentUserId_throwsWhenNotLoggedIn() {
        val ex = assertFailsWith<IllegalStateException> { CurrentUserKit.currentUserId() }
        assertEquals(
            "No logged-in user bound to the current thread: not logged in or not passed through UserContextWebFilter",
            ex.message,
        )
    }

    @Test
    fun currentUserId_throwsWhenUserNotPrincipal() {
        bindUser(object : IIdEntity<String> {
            override val id: String get() = "other"
        })
        assertFailsWith<IllegalStateException> { CurrentUserKit.currentUserId() }
    }

}
