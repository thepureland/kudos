package io.kudos.ms.user.core.org.service

import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.service.iservice.IUserOrgService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.*

/**
 * junit test for UserOrgService
 *
 * Test data source: `UserOrgServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserOrgServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var userOrgService: IUserOrgService

    @Resource
    private lateinit var userOrgDao: UserOrgDao

    @Test
    fun getOrgRecord() {
        val id = "8b4df430-0000-0000-0000-000000000030"
        val cacheItem = userOrgService.getOrgRecord(id)
        assertNotNull(cacheItem)
        assertTrue(cacheItem.name == "svc-org-test-root-1-HuAyup4R")
        
        // Test a non-existent org
        val notExist = userOrgService.getOrgRecord("non-existent-id")
        assertNull(notExist)
    }

    @Test
    fun getOrgsByTenantId() {
        val tenantId = "svc-tenant-org-test-1-HuAyup4R"
        val orgs = userOrgService.getOrgsByTenantId(tenantId)
        assertTrue(orgs.size >= 5) // Only includes active=true
        assertTrue(orgs.any { it.name == "svc-org-test-root-1-HuAyup4R" })
        assertTrue(orgs.any { it.name == "svc-org-test-child-1-HuAyup4R" })
    }

    @Test
    fun getOrgTree() {
        val tenantId = "svc-tenant-org-test-1-HuAyup4R"
        // Test getting the root org tree
        val tree = userOrgService.getOrgTree(tenantId, null)
        assertTrue(tree.isNotEmpty())
        val rootNode = tree.firstOrNull { it.name == "svc-org-test-root-1-HuAyup4R" }
        assertNotNull(rootNode)
        
        // Verify tree structure: child orgs should be in the parent org's children
        val children = assertNotNull(rootNode.children)
        assertTrue(children.isNotEmpty())
        assertTrue(children.any { it.name == "svc-org-test-child-1-HuAyup4R" })
        assertTrue(children.any { it.name == "svc-org-test-child-2-HuAyup4R" })
        
        // Test getting the subtree of a specified parent org (returns child org list directly, does not build the tree)
        val parentId = "8b4df430-0000-0000-0000-000000000030"
        val childTree = userOrgService.getOrgTree(tenantId, parentId)
        assertTrue(childTree.isNotEmpty())
        assertTrue(childTree.any { it.name == "svc-org-test-child-1-HuAyup4R" })
        assertTrue(childTree.any { it.name == "svc-org-test-child-2-HuAyup4R" })
        // When parentId is specified, a flat list is returned, not a tree structure (children is an empty list)
        assertTrue(childTree.all { it.children.isNullOrEmpty() })
    }

    @Test
    fun getAllAncestorOrgIds() {
        val orgId = "8b4df430-0000-0000-0000-000000000033" // grandchild
        val ancestors = userOrgService.getAllAncestorOrgIds(orgId)
        assertTrue(ancestors.size >= 2)
        assertTrue(ancestors.contains("8b4df430-0000-0000-0000-000000000031")) // parent
        assertTrue(ancestors.contains("8b4df430-0000-0000-0000-000000000030")) // grandparent
    }

    @Test
    fun getAllDescendantOrgIds() {
        val orgId = "8b4df430-0000-0000-0000-000000000030" // root
        val descendants = userOrgService.getAllDescendantOrgIds(orgId)
        assertTrue(descendants.size >= 3)
        assertTrue(descendants.contains("8b4df430-0000-0000-0000-000000000031")) // child
        assertTrue(descendants.contains("8b4df430-0000-0000-0000-000000000032")) // child
        assertTrue(descendants.contains("8b4df430-0000-0000-0000-000000000033")) // grandchild
    }

    @Test
    fun updateActive() {
        val id = "8b4df430-0000-0000-0000-000000000030"
        // Set to false first (verify persistence via DAO to avoid assertion failures from un-refreshed cache during batch runs)
        assertTrue(userOrgService.updateActive(id, false))
        var org = userOrgDao.get(id)
        assertNotNull(org)
        assertNotEquals(org.active, true)

        // Set to true again
        assertTrue(userOrgService.updateActive(id, true))
        org = userOrgDao.get(id)
        assertNotNull(org)
        assertEquals(org.active, true)
    }

    @Test
    fun moveOrg() {
        val id = "8b4df430-0000-0000-0000-000000000031"
        val newParentId = "8b4df430-0000-0000-0000-000000000034"
        val newSortNum = 99

        assertTrue(userOrgService.moveOrg(id, newParentId, newSortNum))
        var org = userOrgDao.get(id)
        assertNotNull(org)
        assertEquals(org.parentId, newParentId)
        assertEquals(org.sortNum, newSortNum)

        // Move back to original position (verify persistence via DAO to avoid assertion failures from un-refreshed cache during batch runs)
        assertTrue(userOrgService.moveOrg(id, "8b4df430-0000-0000-0000-000000000030", 11))
        org = userOrgDao.get(id)
        assertNotNull(org)
        assertEquals("8b4df430-0000-0000-0000-000000000030", org.parentId)
        assertEquals(11, org.sortNum)
    }
}
