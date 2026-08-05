package io.kudos.ms.auth.core.role.exclusion.service

import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.auth.core.group.dao.AuthGroupRoleDao
import io.kudos.ms.auth.core.group.dao.AuthGroupUserDao
import io.kudos.ms.auth.core.role.cache.AuthRoleHashCache
import io.kudos.ms.auth.core.role.cache.UserIdsByRoleIdCache
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.exclusion.dao.AuthRoleExclusionDao
import io.kudos.ms.auth.core.role.exclusion.model.po.AuthRoleExclusion
import io.kudos.ms.auth.core.role.exclusion.service.impl.AuthRoleExclusionService
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.lenient
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pure-logic unit test for [AuthRoleExclusionService] (all collaborators mocked with Mockito; no
 * Spring container and no DB).
 *
 * Drives every branch of the SoD mutual-exclusion service through the real implementation,
 * injecting the DAO via the constructor and the `@Resource lateinit var` collaborators by reflection:
 *  - [AuthRoleExclusionService.insert]: canonical ordering (swap when A>B, leave when A<=B), and
 *    every [require] guard in validateBeforeInsert (blank ids, blank tenant, self-exclusion,
 *    missing roleA, missing roleB, tenant mismatch on A, tenant mismatch on B, duplicate pair),
 *    plus the happy path delegating to dao.insert.
 *  - [AuthRoleExclusionService.update]: delegates to dao.update.
 *  - [AuthRoleExclusionService.findByRoleIds]: delegates to dao.searchByRoleIds.
 *  - [AuthRoleExclusionService.findViolatingUserIds]: missing-exclusion empty VO; and the
 *    effective-users intersection across direct / descendant / group paths (both empty and
 *    non-empty descendant/group branches).
 *  - [AuthRoleExclusionService.findViolation]: empty existing set, no relevant rules, a match on
 *    each side of the pair (rule.roleAId == candidate and != candidate), and the no-conflict case.
 *
 * AuthRoleExclusion (a Ktorm entity) is created via its factory and mutated in-memory only — no
 * persistence is involved.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AuthRoleExclusionServiceTest {

    private val dao = mock(AuthRoleExclusionDao::class.java)
    private val authRoleDao = mock(AuthRoleDao::class.java)
    private val authRoleHashCache = mock(AuthRoleHashCache::class.java)
    private val userIdsByRoleIdCache = mock(UserIdsByRoleIdCache::class.java)
    private val authGroupUserDao = mock(AuthGroupUserDao::class.java)
    private val authGroupRoleDao = mock(AuthGroupRoleDao::class.java)

    // roleIdsByUserIdCache is declared in the service but never used by the methods under test;
    // a distinct mock keeps reflection injection happy.
    private val roleIdsCache = mock(io.kudos.ms.auth.core.role.cache.RoleIdsByUserIdCache::class.java)

    private val service = AuthRoleExclusionService(dao).apply {
        inject("authRoleDao", authRoleDao)
        inject("authRoleHashCache", authRoleHashCache)
        inject("authRoleUserDao", mock(io.kudos.ms.auth.core.role.dao.AuthRoleUserDao::class.java))
        inject("userIdsByRoleIdCache", userIdsByRoleIdCache)
        inject("roleIdsByUserIdCache", roleIdsCache)
        inject("authGroupUserDao", authGroupUserDao)
        inject("authGroupRoleDao", authGroupRoleDao)
    }

    private fun AuthRoleExclusionService.inject(field: String, value: Any) {
        val f = AuthRoleExclusionService::class.java.getDeclaredField(field)
        f.isAccessible = true
        f.set(this, value)
    }

    private fun entry(id: String, tenantId: String) = AuthRoleCacheEntry(
        id = id, code = "C_$id", name = "N_$id", tenantId = tenantId, subsysCode = "ams",
        remark = null, active = true, builtIn = false, createUserId = null, createUserName = null,
        createTime = null, updateUserId = null, updateUserName = null, updateTime = null,
    )

    private fun excl(a: String, b: String, tenant: String, id: String? = null) = AuthRoleExclusion {
        this.roleAId = a
        this.roleBId = b
        this.tenantId = tenant
        if (id != null) this.id = id
    }

    // ---------------------------------------------------------------- insert: canonicalisation

    @Test
    fun insert_alreadyCanonical_keepsOrderAndDelegates() {
        val e = excl("a-role", "b-role", "t1")
        whenCalled(authRoleHashCache.getRoleById("a-role")).thenReturn(entry("a-role", "t1"))
        whenCalled(authRoleHashCache.getRoleById("b-role")).thenReturn(entry("b-role", "t1"))
        whenCalled(dao.pairExistsForTenant("a-role", "b-role", "t1")).thenReturn(false)
        whenCalled(dao.insert(e)).thenReturn("new-id")

        val id = service.insert(e)
        assertEquals("new-id", id)
        // A < B already, so order is untouched.
        assertEquals("a-role", e.roleAId)
        assertEquals("b-role", e.roleBId)
        verify(dao).insert(e)
    }

    @Test
    fun insert_reversedOrder_swapsToCanonical() {
        // Supplied B>A: roleAId="z" roleBId="a" must be swapped before validation/insert.
        val e = excl("z-role", "a-role", "t1")
        whenCalled(authRoleHashCache.getRoleById("a-role")).thenReturn(entry("a-role", "t1"))
        whenCalled(authRoleHashCache.getRoleById("z-role")).thenReturn(entry("z-role", "t1"))
        whenCalled(dao.pairExistsForTenant("a-role", "z-role", "t1")).thenReturn(false)
        whenCalled(dao.insert(e)).thenReturn("swapped-id")

        val id = service.insert(e)
        assertEquals("swapped-id", id)
        assertEquals("a-role", e.roleAId, "smaller id must end up in roleAId")
        assertEquals("z-role", e.roleBId)
    }

    @Test
    fun insert_trimsWhitespaceForComparison() {
        // Leading/trailing spaces are trimmed only for the comparison; equal-after-trim keeps order.
        val e = excl("  a-role ", " a-role  ", "t1")
        // After trim both are "a-role" so roleAId<=roleBId is true → no swap, but they are equal →
        // validateBeforeInsert reads the *original* (untrimmed) property values, which differ, so
        // the self-exclusion guard does NOT fire. Stub both lookups.
        lenient().`when`(authRoleHashCache.getRoleById(anyString())).thenReturn(entry("x", "t1"))
        lenient().`when`(dao.pairExistsForTenant(anyString(), anyString(), anyString())).thenReturn(false)
        whenCalled(dao.insert(e)).thenReturn("ok")
        assertEquals("ok", service.insert(e))
    }

    // ---------------------------------------------------------------- insert: validation guards

    @Test
    fun insert_blankRoleA_rejected() {
        val e = excl("", "b-role", "t1")
        val err = assertFailsWith<IllegalArgumentException> { service.insert(e) }
        assertTrue(err.message!!.contains("roleAId must not be blank"))
    }

    @Test
    fun insert_blankRoleB_rejected() {
        // roleAId non-blank, roleBId blank. Canonical: ""(B) < "a"(A)? blank<="a" so swap happens,
        // making roleAId="" → caught by the roleAId-blank guard. Either way an IAE with a blank
        // message is thrown.
        val e = excl("a-role", "", "t1")
        val err = assertFailsWith<IllegalArgumentException> { service.insert(e) }
        assertTrue(err.message!!.contains("must not be blank"))
    }

    @Test
    fun insert_blankTenant_rejected() {
        val e = excl("a-role", "b-role", "")
        val err = assertFailsWith<IllegalArgumentException> { service.insert(e) }
        assertTrue(err.message!!.contains("tenantId must not be blank"))
    }

    @Test
    fun insert_selfExclusion_rejected() {
        val e = excl("same-role", "same-role", "t1")
        val err = assertFailsWith<IllegalArgumentException> { service.insert(e) }
        assertTrue(err.message!!.contains("cannot be mutually exclusive with itself"))
    }

    @Test
    fun insert_roleANotFound_rejected() {
        val e = excl("a-role", "b-role", "t1")
        whenCalled(authRoleHashCache.getRoleById("a-role")).thenReturn(null)
        val err = assertFailsWith<IllegalArgumentException> { service.insert(e) }
        assertTrue(err.message!!.contains("Role not found: a-role"))
    }

    @Test
    fun insert_roleBNotFound_rejected() {
        val e = excl("a-role", "b-role", "t1")
        whenCalled(authRoleHashCache.getRoleById("a-role")).thenReturn(entry("a-role", "t1"))
        whenCalled(authRoleHashCache.getRoleById("b-role")).thenReturn(null)
        val err = assertFailsWith<IllegalArgumentException> { service.insert(e) }
        assertTrue(err.message!!.contains("Role not found: b-role"))
    }

    @Test
    fun insert_roleATenantMismatch_rejected() {
        val e = excl("a-role", "b-role", "t1")
        whenCalled(authRoleHashCache.getRoleById("a-role")).thenReturn(entry("a-role", "OTHER"))
        whenCalled(authRoleHashCache.getRoleById("b-role")).thenReturn(entry("b-role", "t1"))
        val err = assertFailsWith<IllegalArgumentException> { service.insert(e) }
        assertTrue(err.message!!.contains("belongs to tenant OTHER"))
    }

    @Test
    fun insert_roleBTenantMismatch_rejected() {
        val e = excl("a-role", "b-role", "t1")
        whenCalled(authRoleHashCache.getRoleById("a-role")).thenReturn(entry("a-role", "t1"))
        whenCalled(authRoleHashCache.getRoleById("b-role")).thenReturn(entry("b-role", "OTHER"))
        val err = assertFailsWith<IllegalArgumentException> { service.insert(e) }
        assertTrue(err.message!!.contains("Role b-role belongs to tenant OTHER"))
    }

    @Test
    fun insert_duplicatePair_rejected() {
        val e = excl("a-role", "b-role", "t1")
        whenCalled(authRoleHashCache.getRoleById("a-role")).thenReturn(entry("a-role", "t1"))
        whenCalled(authRoleHashCache.getRoleById("b-role")).thenReturn(entry("b-role", "t1"))
        whenCalled(dao.pairExistsForTenant("a-role", "b-role", "t1")).thenReturn(true)
        val err = assertFailsWith<IllegalArgumentException> { service.insert(e) }
        assertTrue(err.message!!.contains("already exists for tenant"))
    }

    // ---------------------------------------------------------------- update / findByRoleIds

    @Test
    fun update_delegatesToSuper() {
        val e = excl("a-role", "b-role", "t1", id = "x1")
        whenCalled(dao.update(e)).thenReturn(true)
        assertTrue(service.update(e))
        verify(dao).update(e)
    }

    @Test
    fun findByRoleIds_delegatesToDao() {
        val rows = listOf(excl("a", "b", "t1", "r1"))
        whenCalled(dao.searchByRoleIds(listOf("a", "b"))).thenReturn(rows)
        assertSame(rows, service.findByRoleIds(listOf("a", "b")))
        verify(dao).searchByRoleIds(listOf("a", "b"))
    }

    // ---------------------------------------------------------------- findViolatingUserIds

    @Test
    fun findViolatingUserIds_missingExclusion_returnsEmptyVo() {
        whenCalled(dao.get("nope")).thenReturn(null)
        val vo = service.findViolatingUserIds("nope")
        assertEquals("nope", vo.exclusionId)
        assertEquals("", vo.roleAId)
        assertEquals("", vo.roleBId)
        assertTrue(vo.violatingUserIds.isEmpty())
    }

    @Test
    fun findViolatingUserIds_intersectsEffectiveUsers_directOnly() {
        val e = excl("roleA", "roleB", "t1", "ex1")
        whenCalled(dao.get("ex1")).thenReturn(e)
        // Direct holders: u1,u2 for A; u2,u3 for B. No descendants, no groups → intersection {u2}.
        whenCalled(userIdsByRoleIdCache.getUserIds("roleA")).thenReturn(listOf("u1", "u2"))
        whenCalled(userIdsByRoleIdCache.getUserIds("roleB")).thenReturn(listOf("u2", "u3"))
        whenCalled(authRoleDao.searchDescendantRoleIds("roleA")).thenReturn(emptySet())
        whenCalled(authRoleDao.searchDescendantRoleIds("roleB")).thenReturn(emptySet())
        whenCalled(authGroupRoleDao.searchGroupIdsByRoleId("roleA")).thenReturn(emptySet())
        whenCalled(authGroupRoleDao.searchGroupIdsByRoleId("roleB")).thenReturn(emptySet())

        val vo = service.findViolatingUserIds("ex1")
        assertEquals("roleA", vo.roleAId)
        assertEquals("roleB", vo.roleBId)
        assertEquals(listOf("u2"), vo.violatingUserIds)
    }

    @Test
    fun findViolatingUserIds_includesDescendantAndGroupPaths() {
        val e = excl("roleA", "roleB", "t1", "ex2")
        whenCalled(dao.get("ex2")).thenReturn(e)
        // roleA effective holders come via a descendant role; roleB via a group.
        whenCalled(userIdsByRoleIdCache.getUserIds("roleA")).thenReturn(emptyList())
        whenCalled(authRoleDao.searchDescendantRoleIds("roleA")).thenReturn(setOf("childA"))
        whenCalled(userIdsByRoleIdCache.getUserIds("childA")).thenReturn(listOf("shared", "onlyA"))
        whenCalled(authGroupRoleDao.searchGroupIdsByRoleId("roleA")).thenReturn(emptySet())

        whenCalled(userIdsByRoleIdCache.getUserIds("roleB")).thenReturn(emptyList())
        whenCalled(authRoleDao.searchDescendantRoleIds("roleB")).thenReturn(emptySet())
        whenCalled(authGroupRoleDao.searchGroupIdsByRoleId("roleB")).thenReturn(setOf("grpB"))
        whenCalled(authGroupUserDao.searchUserIdsByGroupId(anyString(), anyInstant())).thenReturn(setOf("shared", "onlyB"))

        val vo = service.findViolatingUserIds("ex2")
        assertEquals(listOf("shared"), vo.violatingUserIds)
    }

    @Test
    fun findViolatingUserIds_noOverlap_returnsEmptyList() {
        val e = excl("roleA", "roleB", "t1", "ex3")
        whenCalled(dao.get("ex3")).thenReturn(e)
        whenCalled(userIdsByRoleIdCache.getUserIds("roleA")).thenReturn(listOf("u1"))
        whenCalled(userIdsByRoleIdCache.getUserIds("roleB")).thenReturn(listOf("u2"))
        whenCalled(authRoleDao.searchDescendantRoleIds("roleA")).thenReturn(emptySet())
        whenCalled(authRoleDao.searchDescendantRoleIds("roleB")).thenReturn(emptySet())
        whenCalled(authGroupRoleDao.searchGroupIdsByRoleId("roleA")).thenReturn(emptySet())
        whenCalled(authGroupRoleDao.searchGroupIdsByRoleId("roleB")).thenReturn(emptySet())
        assertTrue(service.findViolatingUserIds("ex3").violatingUserIds.isEmpty())
    }

    // ---------------------------------------------------------------- findViolation

    @Test
    fun findViolation_emptyExisting_returnsNull() {
        assertNull(service.findViolation("t1", "cand", emptyList()))
    }

    @Test
    fun findViolation_noRelevantRules_returnsNull() {
        whenCalled(dao.searchByRoleIdAndTenant("cand", "t1")).thenReturn(emptyList())
        assertNull(service.findViolation("t1", "cand", listOf("r1", "r2")))
    }

    @Test
    fun findViolation_candidateIsRoleA_conflictDetected() {
        // rule: A=cand, B=conflict. user already holds "conflict" → violation.
        val rule = excl("cand", "conflict", "t1", "rule1")
        whenCalled(dao.searchByRoleIdAndTenant("cand", "t1")).thenReturn(listOf(rule))
        val hit = service.findViolation("t1", "cand", listOf("conflict", "other"))
        assertSame(rule, hit)
    }

    @Test
    fun findViolation_candidateIsRoleB_conflictDetected() {
        // rule: A=conflict, B=cand. otherSide is roleAId="conflict" which the user holds.
        val rule = excl("conflict", "cand", "t1", "rule2")
        whenCalled(dao.searchByRoleIdAndTenant("cand", "t1")).thenReturn(listOf(rule))
        val hit = service.findViolation("t1", "cand", listOf("conflict"))
        assertSame(rule, hit)
    }

    @Test
    fun findViolation_noConflict_returnsNull() {
        val rule = excl("cand", "conflict", "t1", "rule3")
        whenCalled(dao.searchByRoleIdAndTenant("cand", "t1")).thenReturn(listOf(rule))
        // User does NOT hold the other side.
        assertNull(service.findViolation("t1", "cand", listOf("unrelated")))
    }

    /** Matcher for the non-null `now` parameter; Mockito returns null, Kotlin will not take it. */
    private fun anyInstant(): java.time.LocalDateTime =
        org.mockito.ArgumentMatchers.any(java.time.LocalDateTime::class.java) ?: java.time.LocalDateTime.now()
}
