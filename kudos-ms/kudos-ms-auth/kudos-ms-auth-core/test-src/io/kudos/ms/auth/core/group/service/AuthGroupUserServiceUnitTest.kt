package io.kudos.ms.auth.core.group.service

import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.dao.AuthGroupDao
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.ms.auth.core.group.service.impl.AuthGroupUserService
import io.kudos.ms.auth.core.policy.TenantAdministrationGuard
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import java.time.LocalDateTime
import org.mockito.ArgumentMatchers.anyString
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.context.ApplicationEventPublisher
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure-logic unit test for [AuthGroupUserService] (DAO and event publisher mocked; no Spring, no DB).
 *
 * Read-throughs ([getUserIdsByGroupId], [getGroupIdsByUserId], [exists]) delegate to the DAO untouched.
 * Write paths exercise de-duplication and event semantics:
 *  - batchBind(): empty → 0 with no DAO/event activity; all-already-bound → 0, no insert, no event;
 *    a delta → inserts only the new relations and publishes AuthGroupUserRelationsChanged with exactly
 *    the newly-inserted user ids;
 *  - unbind(): success (publishes the change event) and not-found (count 0 → false, no event).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class AuthGroupUserServiceUnitTest {

    private val dao = mock(AuthGroupUserDao::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)

    private val authGroupRoleDao = mock(AuthGroupRoleDao::class.java)
    private val authGroupDao = mock(AuthGroupDao::class.java)
    private val tenantAdministrationGuard = mock(TenantAdministrationGuard::class.java)
    private val grantPolicyService = mock(IAuthGrantPolicyService::class.java)

    private val service = AuthGroupUserService(dao).apply {
        inject("eventPublisher", eventPublisher)
        // Joining a group grants every role it carries, so membership writes are screened by the
        // same policy service as a direct grant (see AuthGroupUserService.batchBind).
        inject("authGroupRoleDao", authGroupRoleDao)
        inject("authGroupDao", authGroupDao)
        inject("tenantAdministrationGuard", tenantAdministrationGuard)
        inject("grantPolicyService", grantPolicyService)
    }

    init {
        whenCalled(authGroupDao.get(anyString())).thenReturn(group())
    }

    private fun AuthGroupUserService.inject(field: String, value: Any) {
        val f = AuthGroupUserService::class.java.getDeclaredField(field)
        f.isAccessible = true
        f.set(this, value)
    }

    /** Matcher for the non-null `now` parameter; Mockito returns null, Kotlin will not take it. */
    private fun anyInstant(): LocalDateTime =
        ArgumentMatchers.any(LocalDateTime::class.java) ?: LocalDateTime.now()

    @Suppress("UNCHECKED_CAST")
    private fun anyRelationList(): Collection<Any> =
        (ArgumentMatchers.any(Collection::class.java) as Collection<Any>?) ?: emptyList()

    // ---------------------------------------------------------------- read-throughs

    /**
     * The membership reads are evaluated at an instant now (windows and revocation), and the
     * parameter has a default. Kotlin compiles a defaulted call into a static `$default` bridge that
     * Mockito cannot intercept, so a stub written without the argument silently never matches the
     * service's call. Both sides therefore name the matcher explicitly.
     */
    @Test
    fun getUserIdsByGroupId_delegates() {
        whenCalled(dao.searchUserIdsByGroupId(anyString(), anyInstant())).thenReturn(setOf("u1", "u2"))
        assertEquals(setOf("u1", "u2"), service.getUserIdsByGroupId("g1"))
    }

    @Test
    fun getGroupIdsByUserId_delegates() {
        whenCalled(dao.searchGroupIdsByUserId(anyString(), anyInstant())).thenReturn(setOf("g1"))
        assertEquals(setOf("g1"), service.getGroupIdsByUserId("u1"))
    }

    @Test
    fun exists_delegates() {
        whenCalled(dao.exists("g1", "u1")).thenReturn(true)
        assertTrue(service.exists("g1", "u1"))
        whenCalled(dao.exists("g1", "uX")).thenReturn(false)
        assertFalse(service.exists("g1", "uX"))
    }

    // ---------------------------------------------------------------- batchBind

    @Test
    fun batchBind_empty_returnsZero_noDaoNoEvent() {
        assertEquals(0, service.batchBind("g1", emptyList()))
        verify(dao, never()).searchMembershipsByGroupId(anyString())
        verify(dao, never()).batchInsert(anyRelationList(), anyInt())
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    @Test
    fun batchBind_allAlreadyExist_returnsZero_noInsertNoEvent() {
        whenCalled(dao.searchMembershipsByGroupId("g1")).thenReturn(listOf(member("u1"), member("u2")))
        assertEquals(0, service.batchBind("g1", listOf("u1", "u2")))
        verify(dao, never()).batchInsert(anyRelationList(), anyInt())
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    @Test
    fun batchBind_insertsOnlyDelta_andPublishesChangeWithNewIds() {
        whenCalled(dao.searchMembershipsByGroupId("g1")).thenReturn(listOf(member("u1")))
        val count = service.batchBind("g1", listOf("u1", "u2", "u3"))
        assertEquals(2, count)

        val insertCaptor = ArgumentCaptor.forClass(Collection::class.java)
        @Suppress("UNCHECKED_CAST")
        verify(dao).batchInsert((insertCaptor.capture() ?: emptyList<AuthGroupUser>()) as Collection<Any>, anyInt())
        @Suppress("UNCHECKED_CAST")
        val inserted = insertCaptor.value as Collection<AuthGroupUser>
        assertEquals(setOf("u2", "u3"), inserted.map { it.userId }.toSet())
        assertTrue(inserted.all { it.groupId == "g1" })

        val eventCaptor = ArgumentCaptor.forClass(AuthGroupUserRelationsChanged::class.java)
        verify(eventPublisher).publishEvent(
            eventCaptor.capture() ?: AuthGroupUserRelationsChanged("g1", emptyList())
        )
        assertEquals("g1", eventCaptor.value.groupId)
        assertEquals(setOf("u2", "u3"), eventCaptor.value.userIds.toSet())
    }

    // ---------------------------------------------------------------- unbind

    /**
     * Removal is a *soft* revoke (A7): the row stays so that "who was in this group, and who took
     * them out" survives, and so a later re-add reinstates the original membership.
     */
    @Test
    fun unbind_success_publishesChange() {
        whenCalled(dao.searchMembershipsByGroupId("g1")).thenReturn(listOf(member("u1")))
        assertTrue(service.unbind("g1", "u1"))
        verify(eventPublisher).publishEvent(AuthGroupUserRelationsChanged("g1", listOf("u1")))
    }

    @Test
    fun unbind_relationMissing_returnsFalse_noEvent() {
        whenCalled(dao.searchMembershipsByGroupId("g1")).thenReturn(emptyList())
        assertFalse(service.unbind("g1", "uX"))
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    /** An already-revoked row is not a live membership, so removing it again changes nothing. */
    @Test
    fun unbind_alreadyRevoked_returnsFalse_noEvent() {
        whenCalled(dao.searchMembershipsByGroupId("g1")).thenReturn(listOf(member("u1", revoked = true)))
        assertFalse(service.unbind("g1", "u1"))
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    private fun member(userId: String, revoked: Boolean = false) = AuthGroupUser {
        this.id = "row-${userId}"
        this.groupId = "g1"
        this.userId = userId
        this.revoked = revoked
    }

    private fun group() = AuthGroup {
        id = "g1"
        tenantId = "t1"
        subsysCode = "ams"
        code = "GROUP"
        active = true
        builtIn = false
    }
}
