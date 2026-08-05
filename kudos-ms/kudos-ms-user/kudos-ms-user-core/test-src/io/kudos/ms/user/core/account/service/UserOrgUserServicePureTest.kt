package io.kudos.ms.user.core.account.service

import io.kudos.ms.user.core.account.dao.UserOrgUserDao
import io.kudos.ms.user.core.account.event.UserOrgUserAdminUpdated
import io.kudos.ms.user.core.account.event.UserOrgUserRelationsChanged
import io.kudos.ms.user.core.account.model.po.UserOrgUser
import io.kudos.ms.user.core.account.service.impl.UserOrgUserService
import io.kudos.ms.user.core.org.cache.OrgIdsByUserIdCache
import io.kudos.ms.user.core.org.cache.UserIdsByOrgIdCache
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure unit test for [UserOrgUserService] — covers the empty-input, no-op-diff and failure branches that
 * the Docker-gated [UserOrgUserServiceTest] integration test does not reach (empty userIds short-circuit,
 * all-already-bound diff, unbind miss, setOrgAdmin missing relation / update failure).
 *
 * Collaborators are Mockito mocks; `@Autowired` fields are reflection-injected, mirroring the project's
 * existing `MsgPublishServicePureTest` precedent.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserOrgUserServicePureTest {

    private val dao = mock(UserOrgUserDao::class.java)
    private val publishedEvents = mutableListOf<Any>()
    private val recordingPublisher =
        org.springframework.context.ApplicationEventPublisher { e -> publishedEvents.add(e) }
    private val userIdsByOrgIdCache = mock(UserIdsByOrgIdCache::class.java)
    private val orgIdsByUserIdCache = mock(OrgIdsByUserIdCache::class.java)

    private fun build(): UserOrgUserService {
        val svc = UserOrgUserService(dao)
        inject(svc, "userIdsByOrgIdCache", userIdsByOrgIdCache)
        inject(svc, "orgIdsByUserIdCache", orgIdsByUserIdCache)
        inject(svc, "eventPublisher", recordingPublisher)
        return svc
    }

    private fun inject(target: Any, name: String, value: Any) {
        val f = UserOrgUserService::class.java.getDeclaredField(name)
        f.isAccessible = true
        f.set(target, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyCollectionArg(): Collection<Any> =
        (ArgumentMatchers.any(Collection::class.java) as Collection<Any>?) ?: emptyList()

    @AfterTest
    fun clear() = publishedEvents.clear()

    // ---- getters delegate to caches ----

    @Test
    fun getUserIdsByOrgId_returnsCacheValuesAsSet() {
        whenCalled(userIdsByOrgIdCache.getUserIds("o1")).thenReturn(listOf("u1", "u2", "u1"))
        assertEquals(setOf("u1", "u2"), build().getUserIdsByOrgId("o1"))
    }

    @Test
    fun getOrgIdsByUserId_returnsCacheValuesAsSet() {
        whenCalled(orgIdsByUserIdCache.getOrgIds("u1")).thenReturn(listOf("o1", "o2"))
        assertEquals(setOf("o1", "o2"), build().getOrgIdsByUserId("u1"))
    }

    // ---- batchBind ----

    @Test
    fun batchBind_emptyUserIds_returnsZeroWithoutTouchingDao() {
        assertEquals(0, build().batchBind("o1", emptyList(), false))
        verify(dao, never()).searchUserIdsByOrgId(ArgumentMatchers.anyString())
        verify(dao, never()).batchInsert(anyCollectionArg(), ArgumentMatchers.anyInt())
        assertTrue(publishedEvents.isEmpty())
    }

    @Test
    fun batchBind_allAlreadyBound_returnsZeroNoInsertNoEvent() {
        whenCalled(dao.searchUserIdsByOrgId("o1")).thenReturn(listOf("u1", "u2"))
        assertEquals(0, build().batchBind("o1", listOf("u1", "u2"), false))
        verify(dao, never()).batchInsert(anyCollectionArg(), ArgumentMatchers.anyInt())
        assertTrue(publishedEvents.none { it is UserOrgUserRelationsChanged })
    }

    @Test
    fun batchBind_insertsOnlyNewAndPublishesChanged() {
        whenCalled(dao.searchUserIdsByOrgId("o1")).thenReturn(listOf("u1"))
        // batchInsert's return value is not consumed by the service; no stub needed.
        val count = build().batchBind("o1", listOf("u1", "u2", "u3"), true)
        assertEquals(2, count)
        val ev = publishedEvents.filterIsInstance<UserOrgUserRelationsChanged>().single()
        assertEquals("o1", ev.orgId)
        assertEquals(setOf("u2", "u3"), ev.userIds.toSet())
    }

    // ---- unbind ----

    @Test
    fun unbind_success_publishesChanged() {
        whenCalled(dao.deleteByOrgIdAndUserId("o1", "u1")).thenReturn(1)
        assertTrue(build().unbind("o1", "u1"))
        val ev = publishedEvents.filterIsInstance<UserOrgUserRelationsChanged>().single()
        assertEquals("o1", ev.orgId)
        assertEquals(listOf("u1"), ev.userIds.toList())
    }

    @Test
    fun unbind_noRowDeleted_returnsFalseNoEvent() {
        whenCalled(dao.deleteByOrgIdAndUserId("o1", "ghost")).thenReturn(0)
        assertFalse(build().unbind("o1", "ghost"))
        assertTrue(publishedEvents.isEmpty())
    }

    // ---- exists ----

    @Test
    fun exists_delegatesToDao() {
        whenCalled(dao.exists("o1", "u1")).thenReturn(true)
        whenCalled(dao.exists("o1", "ghost")).thenReturn(false)
        val svc = build()
        assertTrue(svc.exists("o1", "u1"))
        assertFalse(svc.exists("o1", "ghost"))
    }

    // ---- setOrgAdmin ----

    @Test
    fun setOrgAdmin_missingRelation_returnsFalseNoUpdateNoEvent() {
        whenCalled(dao.searchByOrgIdAndUserId("o1", "ghost")).thenReturn(emptyList())
        assertFalse(build().setOrgAdmin("o1", "ghost", true))
        verify(dao, never()).update(anyOrgUser())
        assertTrue(publishedEvents.isEmpty())
    }

    @Test
    fun setOrgAdmin_success_publishesAdminUpdated() {
        val rel = mock(UserOrgUser::class.java)
        whenCalled(rel.id).thenReturn("rel-1")
        whenCalled(dao.searchByOrgIdAndUserId("o1", "u1")).thenReturn(listOf(rel))
        whenCalled(dao.update(anyOrgUser())).thenReturn(true)
        assertTrue(build().setOrgAdmin("o1", "u1", true))
        val ev = publishedEvents.filterIsInstance<UserOrgUserAdminUpdated>().single()
        assertEquals("rel-1", ev.id)
        assertEquals("o1", ev.orgId)
    }

    @Test
    fun setOrgAdmin_updateFails_returnsFalseNoEvent() {
        val rel = mock(UserOrgUser::class.java)
        whenCalled(rel.id).thenReturn("rel-1")
        whenCalled(dao.searchByOrgIdAndUserId("o1", "u1")).thenReturn(listOf(rel))
        whenCalled(dao.update(anyOrgUser())).thenReturn(false)
        assertFalse(build().setOrgAdmin("o1", "u1", false))
        assertTrue(publishedEvents.none { it is UserOrgUserAdminUpdated })
    }

    private fun anyOrgUser(): UserOrgUser =
        ArgumentMatchers.any(UserOrgUser::class.java) ?: UserOrgUser { id = "x" }
}
