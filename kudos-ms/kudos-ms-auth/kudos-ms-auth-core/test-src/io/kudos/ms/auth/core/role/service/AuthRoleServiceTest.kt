package io.kudos.ms.auth.core.role.service

import io.kudos.ms.auth.common.role.vo.request.AuthRoleFormCreate
import io.kudos.ms.auth.common.role.vo.request.AuthRoleFormUpdate
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.*

/**
 * junit test for AuthRoleService
 *
 * Test data source: `AuthRoleServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authRoleService: IAuthRoleService

    @Test
    fun getRoleByTenantIdAndCode() {
        val tenantId = "svc-tenant-role-test-1-bq0Y0mrl"
        val roleCode = "svc-role-test-1-bq0Y0mrl"
        val cacheItem = authRoleService.getRoleByTenantIdAndCode(tenantId, roleCode)
        assertNotNull(cacheItem)
        assertEquals(cacheItem.code, roleCode)
        
        // Test a non-existent role.
        val notExist = authRoleService.getRoleByTenantIdAndCode(tenantId, "non-existent")
        assertNull(notExist)
    }

    @Test
    fun getRoleRecord() {
        val id = "249363d1-0000-0000-0000-000000000025"
        val record = authRoleService.getRoleRecord(id)
        assertNotNull(record)
        assertEquals(record.code, "svc-role-test-1-bq0Y0mrl")
        
        // Test a non-existent role.
        val notExist = authRoleService.getRoleRecord("non-existent-id")
        assertNull(notExist)
    }

    @Test
    fun getRolesByTenantId() {
        val tenantId = "svc-tenant-role-test-1-bq0Y0mrl"
        val roles = authRoleService.getRolesByTenantId(tenantId)
        assertTrue(roles.size >= 4)
        assertTrue(roles.any { it.code == "svc-role-test-1-bq0Y0mrl" })
        assertTrue(roles.any { it.code == "svc-role-test-2-bq0Y0mrl" })
    }

    @Test
    fun getRolesBySubSystemCode() {
        val tenantId = "svc-tenant-role-test-1-bq0Y0mrl"
        val subSystemCode = "ams"
        val roles = authRoleService.getRolesBySubsysCode(tenantId, subSystemCode)
        assertTrue(roles.size >= 3)
        assertTrue(roles.any { it.code == "svc-role-test-1-bq0Y0mrl" })
        assertTrue(roles.any { it.code == "svc-role-test-2-bq0Y0mrl" })
        
        // Test another subsystem.
        val subSystemCode2 = "svc-subsys-role-test-1-bq0Y0mrl"
        val roles2 = authRoleService.getRolesBySubsysCode(tenantId, subSystemCode2)
        assertTrue(roles2.any { it.code == "svc-role-test-3-bq0Y0mrl" })
    }

    @Test
    fun updateActive() {
        val id = "249363d1-0000-0000-0000-000000000025"
        // First set to false
        assertTrue(authRoleService.updateActive(id, false))
        var role = authRoleService.getRoleRecord(id)
        assertNotNull(role)
        assertNotEquals(role.active, true)
        
        // Then set to true
        assertTrue(authRoleService.updateActive(id, true))
        role = authRoleService.getRoleRecord(id)
        assertNotNull(role)
        assertEquals(role.active, true)
    }

    @Test
    fun getUsersByRoleCode() {
        val tenantId = "svc-tenant-user-test-1-249363d1"
        val roleCode = "svc-role-user-test-1-249363d1"
        val users = authRoleService.getUsersByRoleCode(tenantId, roleCode)
        assertTrue(users.size >= 2)
        assertTrue(users.any { it.username == "svc-user-test-1-249363d1" })
        assertTrue(users.any { it.username == "svc-user-test-2-249363d1" })
    }

    // -----------------------------------------------------------------------
    // Role inheritance (parent_id): ancestor walk + parent validation.
    // Fixture chain: root(030) <- mid(031) <- leaf(032)
    // -----------------------------------------------------------------------

    private val rootRoleId = "249363d1-0000-0000-0000-000000000030"
    private val midRoleId = "249363d1-0000-0000-0000-000000000031"
    private val leafRoleId = "249363d1-0000-0000-0000-000000000032"
    private val otherTenantRoleId = "249363d1-0000-0000-0000-000000000033"
    private val otherSubsysRoleId = "249363d1-0000-0000-0000-000000000034"

    @Test
    fun getAncestorRoleIds_returnsChainDirectParentFirst() {
        val ancestors = authRoleService.getAncestorRoleIds(leafRoleId)
        // Nearest ancestor first: mid, then root.
        assertEquals(listOf(midRoleId, rootRoleId), ancestors)
    }

    @Test
    fun getAncestorRoleIds_rootHasNoAncestors() {
        assertTrue(authRoleService.getAncestorRoleIds(rootRoleId).isEmpty())
    }

    @Test
    fun update_settingSelfAsParent_rejected() {
        val form = AuthRoleFormUpdate(
            id = leafRoleId, code = null, name = null, tenantId = null,
            subsysCode = null, parentId = leafRoleId, remark = null,
        )
        assertFailsWith<IllegalArgumentException> { authRoleService.update(form) }
    }

    @Test
    fun update_settingDescendantAsParent_rejected() {
        // root's parent = leaf, but leaf is root's descendant -> cycle.
        val form = AuthRoleFormUpdate(
            id = rootRoleId, code = null, name = null, tenantId = null,
            subsysCode = null, parentId = leafRoleId, remark = null,
        )
        assertFailsWith<IllegalArgumentException> { authRoleService.update(form) }
    }

    @Test
    fun update_settingCrossTenantParent_rejected() {
        val form = AuthRoleFormUpdate(
            id = leafRoleId, code = null, name = null, tenantId = null,
            subsysCode = null, parentId = otherTenantRoleId, remark = null,
        )
        assertFailsWith<IllegalArgumentException> { authRoleService.update(form) }
    }

    @Test
    fun update_settingCrossSubsystemParent_rejected() {
        val form = AuthRoleFormUpdate(
            id = leafRoleId, code = null, name = null, tenantId = null,
            subsysCode = null, parentId = otherSubsysRoleId, remark = null,
        )
        assertFailsWith<IllegalArgumentException> { authRoleService.update(form) }
    }

    @Test
    fun update_settingNonExistentParent_rejected() {
        val form = AuthRoleFormUpdate(
            id = leafRoleId, code = null, name = null, tenantId = null,
            subsysCode = null, parentId = "non-existent-parent-id", remark = null,
        )
        assertFailsWith<IllegalArgumentException> { authRoleService.update(form) }
    }

    @Test
    fun update_settingValidAncestorReparent_succeeds() {
        // Re-parent leaf directly under root (skipping mid) — same tenant + subsystem, no cycle.
        // The form is a full replacement, not a patch: BaseCrudDao.update copies every property
        // across (nulls included), so the not-null columns must be restated even when unchanged.
        // The rejection cases above can leave them null because validation trips before the write.
        val form = AuthRoleFormUpdate(
            id = leafRoleId,
            code = "svc-role-hier-leaf-bq0Y0mrl",
            name = "svc-role-hier-leaf-name",
            tenantId = "svc-tenant-hier-1-bq0Y0mrl",
            subsysCode = "ams",
            parentId = rootRoleId,
            remark = null,
        )
        assertTrue(authRoleService.update(form))
        assertEquals(listOf(rootRoleId), authRoleService.getAncestorRoleIds(leafRoleId))
    }

    // -----------------------------------------------------------------------
    // Permission / aggregator reads — role 022 (svc-role-user-test-1) is held by users 016 & 017
    // and grants resources 070 & 071 (see AuthRoleServiceTest.sql).
    // -----------------------------------------------------------------------

    private val roleWithUsers = "249363d1-0000-0000-0000-000000000022"
    private val user016 = "249363d1-0000-0000-0000-000000000016"
    private val user017 = "249363d1-0000-0000-0000-000000000017"
    private val resource070 = "249363d1-0000-0000-0000-000000000070"
    private val resource071 = "249363d1-0000-0000-0000-000000000071"
    private val roleTenant = "svc-tenant-user-test-1-249363d1"
    private val roleCode = "svc-role-user-test-1-249363d1"

    @Test
    fun getRoleUserIds() {
        val userIds = authRoleService.getRoleUserIds(roleWithUsers)
        assertTrue(userIds.containsAll(listOf(user016, user017)))
    }

    @Test
    fun getRoleResourceIds() {
        val resIds = authRoleService.getRoleResourceIds(roleWithUsers)
        assertEquals(setOf(resource070, resource071), resIds)
    }

    @Test
    fun getRoleUsers() {
        val users = authRoleService.getRoleUsers(roleWithUsers)
        val ids = users.map { it.id }.toSet()
        assertTrue(ids.contains(user016) && ids.contains(user017))
        // role with no users -> empty
        assertTrue(authRoleService.getRoleUsers("249363d1-0000-0000-0000-000000000025").isEmpty())
    }

    @Test
    fun getRoleResources() {
        val resources = authRoleService.getRoleResources(roleWithUsers)
        assertEquals(setOf(resource070, resource071), resources.map { it.id }.toSet())
        // role with no resources -> empty
        assertTrue(authRoleService.getRoleResources("249363d1-0000-0000-0000-000000000025").isEmpty())
    }

    @Test
    fun hasResource() {
        assertTrue(authRoleService.hasResource(roleWithUsers, resource070))
        assertFalse(authRoleService.hasResource(roleWithUsers, "no-such-resource"))
    }

    @Test
    fun getUserRoleIds_and_getUserRoles() {
        val roleIds = authRoleService.getUserRoleIds(user016)
        assertTrue(roleIds.contains(roleWithUsers))
        val roles = authRoleService.getUserRoles(user016)
        assertTrue(roles.any { it.id == roleWithUsers })
        // user with no roles -> empty
        assertTrue(authRoleService.getUserRoles("no-such-user").isEmpty())
    }

    @Test
    fun hasRole_and_hasRoleByCode() {
        assertTrue(authRoleService.hasRole(user016, roleWithUsers))
        assertFalse(authRoleService.hasRole(user016, "no-such-role"))
        assertTrue(authRoleService.hasRoleByCode(user016, roleTenant, roleCode))
        // unknown role code -> false (no NPE)
        assertFalse(authRoleService.hasRoleByCode(user016, roleTenant, "no-such-code"))
    }

    @Test
    fun userResourceReads() {
        val resIds = authRoleService.getUserResourceIds(user016)
        assertTrue(resIds.containsAll(setOf(resource070, resource071)))
        assertTrue(authRoleService.isUserHasResource(user016, resource070))
        assertFalse(authRoleService.isUserHasResource(user016, "no-such-resource"))
        val resources = authRoleService.getResources(user016)
        assertTrue(resources.map { it.id }.containsAll(listOf(resource070, resource071)))
    }

    @Test
    fun getEffectivePermissions() {
        val vo = authRoleService.getEffectivePermissions(user016)
        assertTrue(vo.directRoles.any { it.id == roleWithUsers }, "direct role must be present")
        assertTrue(vo.resourcesByRole[roleWithUsers]?.map { it.id }?.toSet() == setOf(resource070, resource071))
        // user with no grants -> empty VO
        val empty = authRoleService.getEffectivePermissions("no-such-user")
        assertTrue(empty.directRoles.isEmpty() && empty.groups.isEmpty())
    }

    @Test
    fun getRoleNamesByResourceIds() {
        val map = authRoleService.getRoleNamesByResourceIds(listOf(resource070))
        assertTrue(map[resource070]?.contains("svc-rol-use-tes-1-name-249363d1") == true)
        // empty input -> empty map
        assertTrue(authRoleService.getRoleNamesByResourceIds(emptyList()).isEmpty())
    }

    @Test
    fun getDeleteImpact() {
        val impact = authRoleService.getDeleteImpact(listOf(roleWithUsers))
        assertTrue(impact.users >= 2, "role is held by at least users 016 & 017")
        // empty input -> zero
        val zero = authRoleService.getDeleteImpact(emptyList())
        assertEquals(0, zero.users)
        assertEquals(0, zero.groups)
    }

    @Test
    fun batchBindUsers_emptyInputs() {
        assertEquals(0, authRoleService.batchBindUsers(emptyList(), listOf(user016)).ok)
        assertEquals(0, authRoleService.batchBindUsers(listOf(roleWithUsers), emptyList()).ok)
    }

    @Test
    fun batchBindUsers_bindsNewUsers() {
        // bind users 016/017 to role 025 (currently empty); both succeed.
        val targetRole = "249363d1-0000-0000-0000-000000000025"
        // Same-tenant users: 016/017 belong to the user fixture's tenant, so binding them here
        // would be a cross-tenant grant, which the policy gate refuses by design.
        val result = authRoleService.batchBindUsers(
            listOf(targetRole),
            listOf("249363d1-0000-0000-0000-00000000008a", "249363d1-0000-0000-0000-00000000008b"),
        )
        assertEquals(1, result.ok, "one role processed successfully")
        assertTrue(result.failures.isEmpty())
        assertTrue(
            authRoleService.getRoleUserIds(targetRole).containsAll(
                listOf("249363d1-0000-0000-0000-00000000008a", "249363d1-0000-0000-0000-00000000008b"),
            ),
        )
    }

    @Test
    fun insert_rootRole_publishesAndPersists() {
        val form = AuthRoleFormCreate(
            code = "svc-role-inserted-new", name = "inserted", tenantId = "svc-tenant-role-test-1-bq0Y0mrl",
            subsysCode = "ams", remark = "inserted by test",
        )
        val id = authRoleService.insert(form)
        assertTrue(id.isNotBlank())
        val record = authRoleService.getRoleRecord(id)
        assertNotNull(record)
        assertEquals("svc-role-inserted-new", record.code)
    }

    @Test
    fun insert_withNonExistentParent_rejected() {
        val form = AuthRoleFormCreate(
            code = "svc-role-bad-parent", name = "bad", tenantId = "svc-tenant-role-test-1-bq0Y0mrl",
            subsysCode = "ams", parentId = "no-such-parent", remark = null,
        )
        assertFailsWith<IllegalArgumentException> { authRoleService.insert(form) }
    }

    @Test
    fun copyRole_copiesMetadataAndResources() {
        val newId = authRoleService.copyRole(
            sourceId = roleWithUsers, code = "svc-role-copy-new", name = "copied", copyResources = true,
        )
        assertTrue(newId.isNotBlank())
        val copy = authRoleService.getRoleRecord(newId)
        assertNotNull(copy)
        assertEquals("svc-role-copy-new", copy.code)
        // resources copied across
        assertEquals(setOf(resource070, resource071), authRoleService.getRoleResourceIds(newId))
    }

    @Test
    fun copyRole_withoutResources() {
        val newId = authRoleService.copyRole(
            sourceId = roleWithUsers, code = "svc-role-copy-nores", name = "copied2", copyResources = false,
        )
        assertTrue(authRoleService.getRoleResourceIds(newId).isEmpty())
    }

    @Test
    fun copyRole_blankCodeOrName_rejected() {
        assertFailsWith<IllegalArgumentException> {
            authRoleService.copyRole(roleWithUsers, code = " ", name = "x", copyResources = false)
        }
        assertFailsWith<IllegalArgumentException> {
            authRoleService.copyRole(roleWithUsers, code = "x", name = " ", copyResources = false)
        }
    }

    @Test
    fun copyRole_sourceNotFound_rejected() {
        assertFailsWith<IllegalArgumentException> {
            authRoleService.copyRole("no-such-source", code = "c", name = "n", copyResources = false)
        }
    }

}
