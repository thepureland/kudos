package io.kudos.ms.user.common.passport

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import org.junit.jupiter.api.AfterEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull


/**
 * Plain JVM unit test — no Docker, no Spring. CurrentUserKit only reads from ThreadLocal, unit tests are sufficient.
 *
 * @author K
 * @since 1.0.0
 */
class CurrentUserKitTest {

    @AfterEach
    fun clearContext() {
        KudosContextHolder.clear()
    }

    @Test
    fun returnsNull_whenContextEmpty() {
        // KudosContextHolder.getOrNull() == null
        assertNull(CurrentUserKit.currentUserIdOrNull())
        assertNull(CurrentUserKit.currentPrincipalOrNull())
        assertNull(CurrentUserKit.currentTenantIdOrNull())
    }

    @Test
    fun returnsNull_whenContextSetButUserMissing() {
        // Passed through WebContextInitFilter but not logged in: context exists, user field is null.
        KudosContextHolder.set(KudosContext())
        assertNull(CurrentUserKit.currentUserIdOrNull())
        assertNull(CurrentUserKit.currentPrincipalOrNull())
    }

    @Test
    fun returnsValues_whenPrincipalSet() {
        val p = SessionUserPrincipal(id = "u-1", tenantId = "t-1", username = "alice")
        KudosContextHolder.get().user = p

        assertEquals("u-1", CurrentUserKit.currentUserIdOrNull())
        assertEquals("alice", CurrentUserKit.currentPrincipalOrNull()?.username)
        assertEquals("t-1", CurrentUserKit.currentTenantIdOrNull())
        assertEquals("u-1", CurrentUserKit.currentUserId())
    }

    @Test
    fun currentUserId_throws_whenNotLoggedIn() {
        assertFailsWith<IllegalStateException> { CurrentUserKit.currentUserId() }
    }

    @Test
    fun returnsNull_whenUserIsNotSessionPrincipalType() {
        // The user field type is IIdEntity<String>?, theoretically other subclasses can be set; this utility uses `as?` cast, non-SessionUserPrincipal should -> null.
        KudosContextHolder.get().user = object : io.kudos.base.model.contract.entity.IIdEntity<String> {
            override val id: String = "x"
        }
        assertNull(CurrentUserKit.currentPrincipalOrNull())
        assertNull(CurrentUserKit.currentUserIdOrNull())
    }
}
