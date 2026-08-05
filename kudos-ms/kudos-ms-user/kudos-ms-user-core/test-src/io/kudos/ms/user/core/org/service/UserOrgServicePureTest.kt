package io.kudos.ms.user.core.org.service

import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.core.account.cache.UserAccountHashCache
import io.kudos.ms.user.core.account.dao.UserOrgUserDao
import io.kudos.ms.user.core.org.cache.UserIdsByOrgIdCache
import io.kudos.ms.user.core.org.cache.UserOrgHashCache
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.event.UserOrgBatchDeleted
import io.kudos.ms.user.core.org.event.UserOrgDeleted
import io.kudos.ms.user.core.org.event.UserOrgInserted
import io.kudos.ms.user.core.org.event.UserOrgUpdated
import io.kudos.ms.user.core.org.model.po.UserOrg
import io.kudos.ms.user.core.org.service.impl.UserOrgService
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure unit test for [UserOrgService] — exercises the in-memory tree / traversal algorithms (getOrgTree,
 * getAllAncestorOrgIds, getAllDescendantOrgIds) and the snapshot-then-publish event branches that the
 * Docker-gated [UserOrgServiceTest] integration test does not deterministically reach.
 *
 * All collaborators are Mockito mocks; `@Autowired`/`@Resource` fields are reflection-injected, mirroring
 * the project's existing [io.kudos.ms.user.core.account.service.UserOrgUserServicePureTest] precedent.
 * No Spring container or DB is required.
 *
 * @author K
 * @since 1.0.0
 */
internal class UserOrgServicePureTest {

    private val dao = mock(UserOrgDao::class.java)
    private val userOrgUserDao = mock(UserOrgUserDao::class.java)
    private val userOrgHashCache = mock(UserOrgHashCache::class.java)
    private val userAccountHashCache = mock(UserAccountHashCache::class.java)
    private val userIdsByOrgIdCache = mock(UserIdsByOrgIdCache::class.java)

    private val publishedEvents = mutableListOf<Any>()
    private val recordingPublisher =
        org.springframework.context.ApplicationEventPublisher { e -> publishedEvents.add(e) }

    private fun build(): UserOrgService {
        val svc = UserOrgService(dao)
        inject(svc, "userOrgUserDao", userOrgUserDao)
        inject(svc, "userOrgHashCache", userOrgHashCache)
        inject(svc, "userAccountHashCache", userAccountHashCache)
        inject(svc, "userIdsByOrgIdCache", userIdsByOrgIdCache)
        inject(svc, "eventPublisher", recordingPublisher)
        return svc
    }

    private fun inject(target: Any, name: String, value: Any) {
        val f = UserOrgService::class.java.getDeclaredField(name)
        f.isAccessible = true
        f.set(target, value)
    }

    private fun anyOrg(): UserOrg = ArgumentMatchers.any(UserOrg::class.java) ?: UserOrg { id = "x" }

    @Suppress("UNCHECKED_CAST")
    private fun anyMapArg(): Map<String, *> =
        (ArgumentMatchers.any(Map::class.java) as Map<String, *>?) ?: emptyMap<String, Any?>()

    private fun cacheEntry(id: String, parentId: String?, sortNum: Int? = null, name: String? = null) =
        UserOrgCacheEntry(
            id = id, name = name, shortName = null, tenantId = "t1", parentId = parentId,
            orgTypeDictCode = null, sortNum = sortNum, remark = null, active = true, builtIn = false,
            createUserId = null, createUserName = null, createTime = null,
            updateUserId = null, updateUserName = null, updateTime = null,
        )

    /**
     * Build several UserOrg PO mocks (id + parentId) eagerly into a list. Building them *before* any
     * `whenCalled(dao...)` stub avoids Mockito's UnfinishedStubbingException, which fires if a new
     * `when()` is started while an outer stubbing is still open.
     */
    private fun orgPos(vararg idToParent: Pair<String, String?>): List<UserOrg> =
        idToParent.map { (id, parent) ->
            val po = mock(UserOrg::class.java)
            whenCalled(po.id).thenReturn(id)
            whenCalled(po.parentId).thenReturn(parent)
            po
        }

    @AfterTest
    fun clear() = publishedEvents.clear()

    // ---- getOrgAdmins / getOrgUsers: empty short-circuit + order-preserving mapNotNull ----

    @Test
    fun getOrgAdmins_emptyAdminIds_returnsEmptyWithoutHittingCache() {
        whenCalled(userOrgUserDao.searchAdminUserIdsByOrgId("o1")).thenReturn(emptyList())
        assertTrue(build().getOrgAdmins("o1").isEmpty())
        verify(userAccountHashCache, never()).getUsersByIds(ArgumentMatchers.anyList())
    }

    @Test
    fun getOrgAdmins_preservesIdOrderAndDropsMissing() {
        val a1 = mock(UserAccountCacheEntry::class.java)
        val a3 = mock(UserAccountCacheEntry::class.java)
        whenCalled(userOrgUserDao.searchAdminUserIdsByOrgId("o1")).thenReturn(listOf("u1", "u2", "u3"))
        whenCalled(userAccountHashCache.getUsersByIds(listOf("u1", "u2", "u3")))
            .thenReturn(mapOf("u3" to a3, "u1" to a1))
        assertEquals(listOf(a1, a3), build().getOrgAdmins("o1"))
    }

    @Test
    fun getOrgUsers_emptyUserIds_returnsEmpty() {
        whenCalled(userIdsByOrgIdCache.getUserIds("o1")).thenReturn(emptyList())
        assertTrue(build().getOrgUsers("o1").isEmpty())
        verify(userAccountHashCache, never()).getUsersByIds(ArgumentMatchers.anyList())
    }

    @Test
    fun isUserInOrg_reflectsCacheMembership() {
        whenCalled(userIdsByOrgIdCache.getUserIds("o1")).thenReturn(listOf("u1", "u2"))
        val svc = build()
        assertTrue(svc.isUserInOrg("u2", "o1"))
        assertFalse(svc.isUserInOrg("u9", "o1"))
    }

    // ---- getParentOrg ----

    @Test
    fun getParentOrg_nullWhenNoParentId() {
        whenCalled(userOrgHashCache.getOrgById("o1")).thenReturn(cacheEntry("o1", parentId = null))
        assertNull(build().getParentOrg("o1"))
    }

    @Test
    fun getParentOrg_resolvesParentViaCache() {
        val parent = cacheEntry("p1", parentId = null)
        whenCalled(userOrgHashCache.getOrgById("o1")).thenReturn(cacheEntry("o1", parentId = "p1"))
        whenCalled(userOrgHashCache.getOrgById("p1")).thenReturn(parent)
        assertEquals(parent, build().getParentOrg("o1"))
    }

    // ---- getChildOrgs: empty short-circuit + order ----

    @Test
    fun getChildOrgs_emptyChildIds_returnsEmpty() {
        whenCalled(dao.searchActiveChildOrgIds("o1")).thenReturn(emptyList())
        assertTrue(build().getChildOrgs("o1").isEmpty())
        verify(userOrgHashCache, never()).getOrgsByIds(ArgumentMatchers.anyCollection())
    }

    // ---- getOrgTree: child-list mode (parentId != null) sorts by sortNum, null last ----

    @Test
    fun getOrgTree_withParentId_returnsFlatListSortedBySortNumNullLast() {
        val pos = orgPos("c1" to "p1", "c2" to "p1", "c3" to "p1")
        whenCalled(dao.searchActiveOrgsByTenantId("t1", "p1")).thenReturn(pos)
        whenCalled(userOrgHashCache.getOrgById("c1")).thenReturn(cacheEntry("c1", "p1", sortNum = 2))
        whenCalled(userOrgHashCache.getOrgById("c2")).thenReturn(cacheEntry("c2", "p1", sortNum = null))
        whenCalled(userOrgHashCache.getOrgById("c3")).thenReturn(cacheEntry("c3", "p1", sortNum = 1))
        val rows = build().getOrgTree("t1", "p1")
        // sortNum: c3(1), c1(2), c2(null -> MAX_VALUE last); no tree nesting in child-list mode.
        assertEquals(listOf("c3", "c1", "c2"), rows.map { it.id })
        assertTrue(rows.all { it.children!!.isEmpty() })
    }

    @Test
    fun getOrgTree_dropsOrgsWithCacheMiss() {
        val pos = orgPos("c1" to "p1", "c2" to "p1")
        whenCalled(dao.searchActiveOrgsByTenantId("t1", "p1")).thenReturn(pos)
        whenCalled(userOrgHashCache.getOrgById("c1")).thenReturn(cacheEntry("c1", "p1", sortNum = 1))
        whenCalled(userOrgHashCache.getOrgById("c2")).thenReturn(null) // cache miss -> dropped
        val rows = build().getOrgTree("t1", "p1")
        assertEquals(listOf("c1"), rows.map { it.id })
    }

    // ---- getOrgTree: full-tree mode (parentId == null) nests children and sorts recursively ----

    @Test
    fun getOrgTree_fullTree_buildsNestedStructureSortedAtEachLevel() {
        // root r1(sort 2), r2(sort 1); r1 has children a(sort 2), b(sort 1).
        val pos = orgPos("r1" to null, "r2" to null, "a" to "r1", "b" to "r1")
        whenCalled(dao.searchActiveOrgsByTenantId("t1", null)).thenReturn(pos)
        whenCalled(userOrgHashCache.getOrgById("r1")).thenReturn(cacheEntry("r1", null, sortNum = 2))
        whenCalled(userOrgHashCache.getOrgById("r2")).thenReturn(cacheEntry("r2", null, sortNum = 1))
        whenCalled(userOrgHashCache.getOrgById("a")).thenReturn(cacheEntry("a", "r1", sortNum = 2))
        whenCalled(userOrgHashCache.getOrgById("b")).thenReturn(cacheEntry("b", "r1", sortNum = 1))
        val roots = build().getOrgTree("t1", null)
        // roots sorted: r2(1), r1(2)
        assertEquals(listOf("r2", "r1"), roots.map { it.id })
        val r2Children = roots[0].children!!
        assertTrue(r2Children.isEmpty())
        // r1's children sorted: b(1), a(2)
        assertEquals(listOf("b", "a"), roots[1].children!!.map { it.id })
    }

    @Test
    fun getOrgTree_fullTree_orphanWithUnknownParentIsNotAttachedNorRoot() {
        // x has parentId "missing-parent" which is not among the node set -> nodeMap lookup null -> dropped from tree.
        val pos = orgPos("r1" to null, "x" to "missing-parent")
        whenCalled(dao.searchActiveOrgsByTenantId("t1", null)).thenReturn(pos)
        whenCalled(userOrgHashCache.getOrgById("r1")).thenReturn(cacheEntry("r1", null, sortNum = 1))
        whenCalled(userOrgHashCache.getOrgById("x")).thenReturn(cacheEntry("x", "missing-parent", sortNum = 1))
        val roots = build().getOrgTree("t1", null)
        assertEquals(listOf("r1"), roots.map { it.id })
        assertTrue(roots[0].children!!.isEmpty())
    }

    // ---- getAllAncestorOrgIds: walk up parentId chain ----

    @Test
    fun getAllAncestorOrgIds_missingRoot_returnsEmpty() {
        whenCalled(userOrgHashCache.getOrgById("o1")).thenReturn(null)
        assertTrue(build().getAllAncestorOrgIds("o1").isEmpty())
    }

    @Test
    fun getAllAncestorOrgIds_walksChainToRoot() {
        whenCalled(userOrgHashCache.getOrgById("o3")).thenReturn(cacheEntry("o3", parentId = "o2"))
        whenCalled(userOrgHashCache.getOrgById("o2")).thenReturn(cacheEntry("o2", parentId = "o1"))
        whenCalled(userOrgHashCache.getOrgById("o1")).thenReturn(cacheEntry("o1", parentId = null))
        assertEquals(listOf("o2", "o1"), build().getAllAncestorOrgIds("o3"))
    }

    @Test
    fun getAllAncestorOrgIds_keepsLastParentIdWhenCacheMissesMidChain() {
        // o3 -> parent o2, but o2 is not in cache: the loop still keeps o2 then breaks (spans cache boundary).
        whenCalled(userOrgHashCache.getOrgById("o3")).thenReturn(cacheEntry("o3", parentId = "o2"))
        whenCalled(userOrgHashCache.getOrgById("o2")).thenReturn(null)
        assertEquals(listOf("o2"), build().getAllAncestorOrgIds("o3"))
    }

    // ---- getAllDescendantOrgIds: BFS over child-id lists ----

    @Test
    fun getAllDescendantOrgIds_bfsCollectsAllLevels() {
        whenCalled(dao.searchActiveChildOrgIds("root")).thenReturn(listOf("a", "b"))
        whenCalled(dao.searchActiveChildOrgIds("a")).thenReturn(listOf("a1"))
        whenCalled(dao.searchActiveChildOrgIds("b")).thenReturn(emptyList())
        whenCalled(dao.searchActiveChildOrgIds("a1")).thenReturn(emptyList())
        assertEquals(listOf("a", "b", "a1"), build().getAllDescendantOrgIds("root"))
    }

    @Test
    fun getAllDescendantOrgIds_noChildren_returnsEmpty() {
        whenCalled(dao.searchActiveChildOrgIds("leaf")).thenReturn(emptyList())
        assertTrue(build().getAllDescendantOrgIds("leaf").isEmpty())
    }

    // ---- updateActive: snapshots parentId into UserOrgUpdated (old == new) ----

    @Test
    fun updateActive_success_publishesUpdatedWithSameParentSnapshot() {
        val po = cachePo("o1", parentId = "p1")
        whenCalled(dao.get("o1")).thenReturn(po)
        whenCalled(dao.updateProperties(ArgumentMatchers.eq("o1") ?: "o1", anyMapArg())).thenReturn(true)
        assertTrue(build().updateActive("o1", false))
        val ev = publishedEvents.filterIsInstance<UserOrgUpdated>().single()
        assertEquals("o1", ev.id)
        assertEquals("p1", ev.oldParentId)
        assertEquals("p1", ev.newParentId)
    }

    @Test
    fun updateActive_daoFalse_noEvent() {
        val po = cachePo("o1", parentId = "p1")
        whenCalled(dao.get("o1")).thenReturn(po)
        whenCalled(dao.updateProperties(ArgumentMatchers.eq("o1") ?: "o1", anyMapArg())).thenReturn(false)
        assertFalse(build().updateActive("o1", false))
        assertTrue(publishedEvents.none { it is UserOrgUpdated })
    }

    // ---- moveOrg: oldParentId snapshot vs newParentId ----

    @Test
    fun moveOrg_success_publishesOldAndNewParent() {
        val po = cachePo("o1", parentId = "old-p")
        whenCalled(dao.get("o1")).thenReturn(po)
        whenCalled(dao.updateProperties(ArgumentMatchers.eq("o1") ?: "o1", anyMapArg())).thenReturn(true)
        assertTrue(build().moveOrg("o1", "new-p", 5))
        val ev = publishedEvents.filterIsInstance<UserOrgUpdated>().single()
        assertEquals("old-p", ev.oldParentId)
        assertEquals("new-p", ev.newParentId)
    }

    @Test
    fun moveOrg_failure_noEvent() {
        val po = cachePo("o1", parentId = "old-p")
        whenCalled(dao.get("o1")).thenReturn(po)
        whenCalled(dao.updateProperties(ArgumentMatchers.eq("o1") ?: "o1", anyMapArg())).thenReturn(false)
        assertFalse(build().moveOrg("o1", "new-p", null))
        assertTrue(publishedEvents.isEmpty())
    }

    // ---- insert / delete / batchDelete overrides ----

    @Test
    fun insert_publishesInsertedEvent() {
        val po = UserOrg { id = "new-id" }
        whenCalled(dao.insert(po)).thenReturn("new-id")
        assertEquals("new-id", build().insert(po))
        assertEquals(1, publishedEvents.count { it is UserOrgInserted && it.id == "new-id" })
    }

    @Test
    fun deleteById_missing_returnsFalseNoEvent() {
        whenCalled(dao.get("ghost")).thenReturn(null)
        assertFalse(build().deleteById("ghost"))
        verify(dao, never()).deleteById(ArgumentMatchers.anyString())
        assertTrue(publishedEvents.isEmpty())
    }

    @Test
    fun deleteById_success_publishesDeletedWithParentSnapshot() {
        val po = cachePo("o1", parentId = "p1")
        whenCalled(dao.get("o1")).thenReturn(po)
        whenCalled(dao.deleteById("o1")).thenReturn(true)
        assertTrue(build().deleteById("o1"))
        val ev = publishedEvents.filterIsInstance<UserOrgDeleted>().single()
        assertEquals("o1", ev.id)
        assertEquals("p1", ev.parentId)
    }

    @Test
    fun deleteById_daoFalse_noEvent() {
        val po = cachePo("o1", parentId = "p1")
        whenCalled(dao.get("o1")).thenReturn(po)
        whenCalled(dao.deleteById("o1")).thenReturn(false)
        assertFalse(build().deleteById("o1"))
        assertTrue(publishedEvents.none { it is UserOrgDeleted })
    }

    @Test
    fun batchDelete_empty_returnsZeroNoEvent() {
        whenCalled(dao.batchDelete(emptyList())).thenReturn(0)
        assertEquals(0, build().batchDelete(emptyList()))
        assertTrue(publishedEvents.none { it is UserOrgBatchDeleted })
    }

    @Test
    fun batchDelete_nonEmpty_snapshotsThenPublishesBatchDeleted() {
        val o1 = cachePo("o1", parentId = "p1")
        val o2 = cachePo("o2", parentId = null)
        whenCalled(dao.getByIds(listOf("o1", "o2"))).thenReturn(listOf(o1, o2))
        whenCalled(dao.batchDelete(listOf("o1", "o2"))).thenReturn(2)
        assertEquals(2, build().batchDelete(listOf("o1", "o2")))
        val ev = publishedEvents.filterIsInstance<UserOrgBatchDeleted>().single()
        assertEquals(listOf("o1", "o2"), ev.ids.toList())
    }

    /** A mock UserOrg PO exposing id + parentId (used by the snapshot-then-publish paths). */
    private fun cachePo(id: String, parentId: String?): UserOrg {
        val po = mock(UserOrg::class.java)
        whenCalled(po.id).thenReturn(id)
        whenCalled(po.parentId).thenReturn(parentId)
        return po
    }
}
