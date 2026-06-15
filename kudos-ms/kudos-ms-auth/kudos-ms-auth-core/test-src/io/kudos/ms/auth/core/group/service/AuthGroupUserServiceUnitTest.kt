package io.kudos.ms.auth.core.group.service

import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.group.event.AuthGroupUserRelationsChanged
import io.kudos.ms.auth.core.group.model.po.AuthGroupUser
import io.kudos.ms.auth.core.group.service.impl.AuthGroupUserService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
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
 * @since 1.0.0
 */
internal class AuthGroupUserServiceUnitTest {

    private val dao = mock(AuthGroupUserDao::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)

    private val service = AuthGroupUserService(dao).apply {
        val f = AuthGroupUserService::class.java.getDeclaredField("eventPublisher")
        f.isAccessible = true
        f.set(this, eventPublisher)
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyRelationList(): Collection<Any> =
        (ArgumentMatchers.any(Collection::class.java) as Collection<Any>?) ?: emptyList()

    // ---------------------------------------------------------------- read-throughs

    @Test
    fun getUserIdsByGroupId_delegates() {
        whenCalled(dao.searchUserIdsByGroupId("g1")).thenReturn(setOf("u1", "u2"))
        assertEquals(setOf("u1", "u2"), service.getUserIdsByGroupId("g1"))
    }

    @Test
    fun getGroupIdsByUserId_delegates() {
        whenCalled(dao.searchGroupIdsByUserId("u1")).thenReturn(setOf("g1"))
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
        verify(dao, never()).searchUserIdsByGroupId(anyString())
        verify(dao, never()).batchInsert(anyRelationList(), anyInt())
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    @Test
    fun batchBind_allAlreadyExist_returnsZero_noInsertNoEvent() {
        whenCalled(dao.searchUserIdsByGroupId("g1")).thenReturn(setOf("u1", "u2"))
        assertEquals(0, service.batchBind("g1", listOf("u1", "u2")))
        verify(dao, never()).batchInsert(anyRelationList(), anyInt())
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    @Test
    fun batchBind_insertsOnlyDelta_andPublishesChangeWithNewIds() {
        whenCalled(dao.searchUserIdsByGroupId("g1")).thenReturn(setOf("u1"))
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

    @Test
    fun unbind_success_publishesChange() {
        whenCalled(dao.deleteByGroupIdAndUserId("g1", "u1")).thenReturn(1)
        assertTrue(service.unbind("g1", "u1"))
        verify(eventPublisher).publishEvent(AuthGroupUserRelationsChanged("g1", listOf("u1")))
    }

    @Test
    fun unbind_relationMissing_returnsFalse_noEvent() {
        whenCalled(dao.deleteByGroupIdAndUserId("g1", "uX")).thenReturn(0)
        assertFalse(service.unbind("g1", "uX"))
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }
}
