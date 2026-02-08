package io.kudos.ms.user.core.service

import io.kudos.ms.user.core.dao.UserOrgDao
import io.kudos.ms.user.core.service.iservice.IUserOrgService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.*

/**
 * junit test for UserOrgService
 *
 * 测试数据来源：`UserOrgServiceTest.sql`
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
        
        // 测试不存在的机构
        val notExist = userOrgService.getOrgRecord("non-existent-id")
        assertNull(notExist)
    }

    @Test
    fun getOrgsByTenantId() {
        val tenantId = "svc-tenant-org-test-1-HuAyup4R"
        val orgs = userOrgService.getOrgsByTenantId(tenantId)
        assertTrue(orgs.size >= 5) // 只包含active=true的
        assertTrue(orgs.any { it.name == "svc-org-test-root-1-HuAyup4R" })
        assertTrue(orgs.any { it.name == "svc-org-test-child-1-HuAyup4R" })
    }

    @Test
    fun getOrgTree() {
        val tenantId = "svc-tenant-org-test-1-HuAyup4R"
        // 测试获取根机构树
        val tree = userOrgService.getOrgTree(tenantId, null)
        assertTrue(tree.isNotEmpty())
        val rootNode = tree.firstOrNull { it.name == "svc-org-test-root-1-HuAyup4R" }
        assertNotNull(rootNode)
        
        // 验证树结构：子机构应该在父机构的children中
        assertNotNull(rootNode.children)
        assertTrue(rootNode.children!!.isNotEmpty())
        assertTrue(rootNode.children!!.any { it.name == "svc-org-test-child-1-HuAyup4R" })
        assertTrue(rootNode.children!!.any { it.name == "svc-org-test-child-2-HuAyup4R" })
        
        // 测试获取指定父机构的子树（直接返回子机构列表，不构建树）
        val parentId = "8b4df430-0000-0000-0000-000000000030"
        val childTree = userOrgService.getOrgTree(tenantId, parentId)
        assertTrue(childTree.isNotEmpty())
        assertTrue(childTree.any { it.name == "svc-org-test-child-1-HuAyup4R" })
        assertTrue(childTree.any { it.name == "svc-org-test-child-2-HuAyup4R" })
        // 当指定parentId时，返回的是平铺列表，不是树结构（children为空列表）
        assertTrue(childTree.all { it.children == null || it.children!!.isEmpty() })
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
        // 先设置为false（用 DAO 校验持久化，避免批量跑时缓存未刷新导致断言失败）
        assertTrue(userOrgService.updateActive(id, false))
        var org = userOrgDao.getAs(id)
        assertNotNull(org)
        assertNotEquals(org.active, true)

        // 再设置为true
        assertTrue(userOrgService.updateActive(id, true))
        org = userOrgDao.getAs(id)
        assertNotNull(org)
        assertEquals(org.active, true)
    }

    @Test
    fun moveOrg() {
        val id = "8b4df430-0000-0000-0000-000000000031"
        val newParentId = "8b4df430-0000-0000-0000-000000000034"
        val newSortNum = 99

        assertTrue(userOrgService.moveOrg(id, newParentId, newSortNum))
        var org = userOrgDao.getAs(id)
        assertNotNull(org)
        assertEquals(org.parentId, newParentId)
        assertEquals(org.sortNum, newSortNum)

        // 移回原位置（用 DAO 校验持久化，避免批量跑时缓存未刷新导致断言失败）
        assertTrue(userOrgService.moveOrg(id, "8b4df430-0000-0000-0000-000000000030", 11))
        org = userOrgDao.getAs(id)
        assertNotNull(org)
        assertEquals("8b4df430-0000-0000-0000-000000000030", org.parentId)
        assertEquals(11, org.sortNum)
    }
}
