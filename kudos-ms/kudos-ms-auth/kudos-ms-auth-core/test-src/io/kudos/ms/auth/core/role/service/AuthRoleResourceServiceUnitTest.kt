package io.kudos.ms.auth.core.role.service

import io.kudos.ms.auth.core.platform.cache.ResourceIdsByRoleIdCache
import io.kudos.ms.auth.core.role.dao.AuthRoleResourceDao
import io.kudos.ms.auth.core.role.event.AuthRoleResourceRelationsChanged
import io.kudos.ms.auth.core.role.model.po.AuthRoleResource
import io.kudos.ms.auth.core.role.service.impl.AuthRoleResourceService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
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
 * Pure-logic unit test for [AuthRoleResourceService] (DAO/cache/event-publisher mocked with
 * Mockito; no Spring container and no DB) — complements the container-backed
 * [AuthRoleResourceServiceTest] by isolating the branches that do not need persistence:
 *
 *  - [AuthRoleResourceService.batchBind]: empty-input short-circuit; the trim-then-dedup against
 *    existing relations (padded `character(N)` resource ids are trimmed on both sides); the
 *    all-already-bound short-circuit (no insert, no event); and the happy path verifying the
 *    inserted POs carry trimmed ids and a single change event is published with exactly the
 *    newly-bound ids.
 *  - [AuthRoleResourceService.unbind]: success (delete>0 ⇒ event published with the trimmed id)
 *    and the no-op branch (delete==0 ⇒ false, no event).
 *  - [AuthRoleResourceService.exists]: the resourceId is trimmed before hitting the DAO.
 *  - [AuthRoleResourceService.getResourceIdsByRoleId] / [getRoleIdsByResourceId]: delegation and
 *    Set conversion.
 *
 * The DAO is supplied through the [io.kudos.base.support.service.impl.BaseCrudService] constructor;
 * the `@Autowired lateinit var` collaborators are injected by reflection (same approach the
 * existing exclusion/grant unit tests use).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AuthRoleResourceServiceUnitTest {

    private val dao = mock(AuthRoleResourceDao::class.java)
    private val cache = mock(ResourceIdsByRoleIdCache::class.java)
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)

    private val service = AuthRoleResourceService(dao).apply {
        inject("resourceIdsByRoleIdCache", cache)
        inject("eventPublisher", eventPublisher)
    }

    private fun AuthRoleResourceService.inject(field: String, value: Any) {
        val f = AuthRoleResourceService::class.java.getDeclaredField(field)
        f.isAccessible = true
        f.set(this, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyRelationCollection(): Collection<Any> =
        (ArgumentMatchers.any(Collection::class.java) as Collection<Any>?) ?: emptyList()

    private fun captureEvent(captor: ArgumentCaptor<AuthRoleResourceRelationsChanged>): Any =
        captor.capture() ?: AuthRoleResourceRelationsChanged("x", emptyList())

    // ---------------------------------------------------------------- batchBind

    @Test
    fun batchBind_emptyInput_returnsZeroNoSideEffects() {
        assertEquals(0, service.batchBind("role1", emptyList()))
        verify(dao, never()).searchResourceIdsByRoleIds(ArgumentMatchers.anyList())
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    @Test
    fun batchBind_allAlreadyBound_returnsZeroNoInsertNoEvent() {
        // Existing returns the same (padded) ids as requested → after trim, candidate set is empty.
        whenCalled(dao.searchResourceIdsByRoleIds(listOf("role1"))).thenReturn(setOf("res1  ", "res2"))
        assertEquals(0, service.batchBind("role1", listOf("res1", " res2 ")))
        verify(dao, never()).batchInsert(anyRelationCollection(), anyInt())
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    @Test
    fun batchBind_newResources_trimsInsertsAndPublishesEvent() {
        // res1 already bound (with padding); res2/res3 are new and arrive with surrounding spaces.
        whenCalled(dao.searchResourceIdsByRoleIds(listOf("role1"))).thenReturn(setOf("res1   "))
        whenCalled(dao.batchInsert(anyRelationCollection(), anyInt())).thenReturn(2)

        val bound = service.batchBind("role1", listOf(" res1 ", "res2 ", " res3"))
        assertEquals(2, bound)

        // Captured inserted POs must carry trimmed resource ids and the right roleId.
        @Suppress("UNCHECKED_CAST")
        val relCaptor = ArgumentCaptor.forClass(Collection::class.java) as ArgumentCaptor<Collection<Any>>
        verify(dao).batchInsert(relCaptor.capture() ?: emptyList(), anyInt())
        val inserted = relCaptor.value.map { it as AuthRoleResource }
        assertEquals(setOf("res2", "res3"), inserted.mapTo(mutableSetOf()) { it.resourceId })
        assertTrue(inserted.all { it.roleId == "role1" })

        val evtCaptor = ArgumentCaptor.forClass(AuthRoleResourceRelationsChanged::class.java)
        verify(eventPublisher).publishEvent(captureEvent(evtCaptor))
        assertEquals("role1", evtCaptor.value.roleId)
        assertEquals(setOf("res2", "res3"), evtCaptor.value.resourceIds.toSet())
    }

    // ---------------------------------------------------------------- unbind

    @Test
    fun unbind_deleted_publishesEventWithTrimmedId() {
        whenCalled(dao.deleteByRoleIdAndResourceId("role1", "res9")).thenReturn(1)
        assertTrue(service.unbind("role1", "  res9 "))
        val captor = ArgumentCaptor.forClass(AuthRoleResourceRelationsChanged::class.java)
        verify(eventPublisher).publishEvent(captureEvent(captor))
        assertEquals(listOf("res9"), captor.value.resourceIds.toList())
    }

    @Test
    fun unbind_nothingDeleted_returnsFalseNoEvent() {
        whenCalled(dao.deleteByRoleIdAndResourceId("role1", "ghost")).thenReturn(0)
        assertFalse(service.unbind("role1", "ghost"))
        verify(eventPublisher, never()).publishEvent(ArgumentMatchers.any())
    }

    // ---------------------------------------------------------------- exists / getters

    @Test
    fun exists_trimsResourceIdBeforeDao() {
        whenCalled(dao.exists("role1", "res5")).thenReturn(true)
        assertTrue(service.exists("role1", " res5  "))
        verify(dao).exists("role1", "res5")
    }

    @Test
    fun getResourceIdsByRoleId_delegatesToCacheAsSet() {
        whenCalled(cache.getResourceIds("role1")).thenReturn(listOf("a", "b", "a"))
        assertEquals(setOf("a", "b"), service.getResourceIdsByRoleId("role1"))
    }

    @Test
    fun getRoleIdsByResourceId_delegatesToDao() {
        whenCalled(dao.searchRoleIdsByResourceId("res1")).thenReturn(setOf("r1", "r2"))
        assertEquals(setOf("r1", "r2"), service.getRoleIdsByResourceId("res1"))
    }
}
