package io.kudos.ams.auth.provider.user.service

import io.kudos.ams.auth.provider.user.service.iservice.IAuthDeptService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.*

/**
 * junit test for AuthDeptService
 *
 * 测试数据来源：`AuthDeptServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthDeptServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var authDeptService: IAuthDeptService

    @Test
    fun getDeptRecord() {
        val id = "8b4df430-0000-0000-0000-000000000030"
        val cacheItem = authDeptService.getDeptRecord(id)
        assertNotNull(cacheItem)
        assertTrue(cacheItem.name == "svc-dept-test-root-1-HuAyup4R")
        
        // 测试不存在的部门
        val notExist = authDeptService.getDeptRecord("non-existent-id")
        assertNull(notExist)
    }

    @Test
    fun getDeptsByTenantId() {
        val tenantId = "svc-tenant-dept-test-1-HuAyup4R"
        val depts = authDeptService.getDeptsByTenantId(tenantId)
        assertTrue(depts.size >= 5) // 只包含active=true的
        assertTrue(depts.any { it.name == "svc-dept-test-root-1-HuAyup4R" })
        assertTrue(depts.any { it.name == "svc-dept-test-child-1-HuAyup4R" })
    }

    @Test
    fun getDeptTree() {
        val tenantId = "svc-tenant-dept-test-1-HuAyup4R"
        // 测试获取根部门树
        val tree = authDeptService.getDeptTree(tenantId, null)
        assertTrue(tree.isNotEmpty())
        val rootNode = tree.firstOrNull { it.name == "svc-dept-test-root-1-HuAyup4R" }
        assertNotNull(rootNode)
        
        // 验证树结构：子部门应该在父部门的children中
        assertNotNull(rootNode.children)
        assertTrue(rootNode.children!!.isNotEmpty())
        assertTrue(rootNode.children!!.any { it.name == "svc-dept-test-child-1-HuAyup4R" })
        assertTrue(rootNode.children!!.any { it.name == "svc-dept-test-child-2-HuAyup4R" })
        
        // 测试获取指定父部门的子树（直接返回子部门列表，不构建树）
        val parentId = "8b4df430-0000-0000-0000-000000000030"
        val childTree = authDeptService.getDeptTree(tenantId, parentId)
        assertTrue(childTree.isNotEmpty())
        assertTrue(childTree.any { it.name == "svc-dept-test-child-1-HuAyup4R" })
        assertTrue(childTree.any { it.name == "svc-dept-test-child-2-HuAyup4R" })
        // 当指定parentId时，返回的是平铺列表，不是树结构（children为空列表）
        assertTrue(childTree.all { it.children == null || it.children!!.isEmpty() })
    }

    @Test
    fun getAllAncestorDeptIds() {
        val deptId = "8b4df430-0000-0000-0000-000000000033" // grandchild
        val ancestors = authDeptService.getAllAncestorDeptIds(deptId)
        assertTrue(ancestors.size >= 2)
        assertTrue(ancestors.contains("8b4df430-0000-0000-0000-000000000031")) // parent
        assertTrue(ancestors.contains("8b4df430-0000-0000-0000-000000000030")) // grandparent
    }

    @Test
    fun getAllDescendantDeptIds() {
        val deptId = "8b4df430-0000-0000-0000-000000000030" // root
        val descendants = authDeptService.getAllDescendantDeptIds(deptId)
        assertTrue(descendants.size >= 3)
        assertTrue(descendants.contains("8b4df430-0000-0000-0000-000000000031")) // child
        assertTrue(descendants.contains("8b4df430-0000-0000-0000-000000000032")) // child
        assertTrue(descendants.contains("8b4df430-0000-0000-0000-000000000033")) // grandchild
    }

    @Test
    fun updateActive() {
        val id = "8b4df430-0000-0000-0000-000000000030"
        // 先设置为false
        assertTrue(authDeptService.updateActive(id, false))
        var dept = authDeptService.getDeptRecord(id)
        assertNotNull(dept)
        assertFalse(dept.active == true)
        
        // 再设置为true
        assertTrue(authDeptService.updateActive(id, true))
        dept = authDeptService.getDeptRecord(id)
        assertNotNull(dept)
        assertTrue(dept.active == true)
    }

    @Test
    fun moveDept() {
        val id = "8b4df430-0000-0000-0000-000000000031"
        val newParentId = "8b4df430-0000-0000-0000-000000000034"
        val newSortNum = 99
        
        assertTrue(authDeptService.moveDept(id, newParentId, newSortNum))
        val dept = authDeptService.getDeptRecord(id)
        assertNotNull(dept)
        assertTrue(dept.parentId == newParentId)
        assertTrue(dept.sortNum == newSortNum)
        
        // 移回原位置
        assertTrue(authDeptService.moveDept(id, "8b4df430-0000-0000-0000-000000000030", 11))
    }
}
