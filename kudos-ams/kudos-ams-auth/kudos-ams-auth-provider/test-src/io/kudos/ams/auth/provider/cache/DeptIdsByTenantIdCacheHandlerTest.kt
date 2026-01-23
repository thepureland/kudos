package io.kudos.ams.auth.provider.cache

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ams.auth.provider.dao.AuthDeptDao
import io.kudos.ams.auth.provider.model.po.AuthDept
import io.kudos.test.container.cache.RdbAndRedisCacheTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for DeptIdsByTenantIdCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class DeptIdsByTenantIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    init {
        cacheStrategyHolder.value = CacheStrategy.LOCAL_REMOTE.name
    }

    @Resource
    private lateinit var cacheHandler: DeptIdsByTenantIdCacheHandler

    @Resource
    private lateinit var authDeptDao: AuthDeptDao

    @Test
    fun getDeptIds() {
        // 存在的租户（有多个部门）- 使用 V1.0.0.11__DeptIdsByTenantIdCacheHandlerTest.sql 的测试数据
        var tenantId = "tenant-001"
        val deptIds1 = cacheHandler.getDeptIds(tenantId)
        val deptIds2 = cacheHandler.getDeptIds(tenantId)
        assertEquals(7, deptIds1.size, "tenant-001 应该有 7 个 active=true 的部门（技术部、产品部、运营部、前端组、后端组、测试组、人事部）")
        assertTrue(deptIds1.contains("11111111-1111-1111-1111-111111111111"), "应该包含技术部ID") // 技术部
        assertTrue(deptIds1.contains("22222222-2222-2222-2222-222222222222"), "应该包含产品部ID") // 产品部
        assertTrue(deptIds1.contains("88888888-8888-8888-8888-888888888888"), "应该包含人事部ID") // 人事部
        assertEquals(deptIds1, deptIds2, "两次调用应该返回相同的结果（缓存验证）")

        // 存在的租户（只有 1 个部门）
        tenantId = "tenant-002"
        val deptIds3 = cacheHandler.getDeptIds(tenantId)
        assertEquals(1, deptIds3.size, "tenant-002 应该有 1 个 active=true 的部门（总部）")
        assertTrue(deptIds3.contains("99999999-9999-9999-9999-999999999999"), "应该包含总部ID") // 总部

        // 不存在的租户
        tenantId = "no_exist_tenant"
        val deptIds4 = cacheHandler.getDeptIds(tenantId)
        assertTrue(deptIds4.isEmpty(), "不存在的租户应该返回空列表")
    }

    @Test
    fun syncOnInsert() {
        val tenantId = "tenant-001"
        val authDept = AuthDept().apply {
            this.tenantId = tenantId
            this.name = "测试部门_${System.currentTimeMillis()}"
            this.deptTypeDictCode = "DEPT_TYPE_DEFAULT"
            this.active = true
        }
        val id = authDeptDao.insert(authDept)

        try {
            // 先获取一次，记录部门数量
            val deptIdsBefore = cacheHandler.getDeptIds(tenantId)
            val beforeSize = deptIdsBefore.size

            // 同步缓存
            cacheHandler.syncOnInsert(authDept, id)

            // 验证新记录是否在缓存中
            val deptIdsAfter = cacheHandler.getDeptIds(tenantId)
            assertTrue(deptIdsAfter.contains(id), "新部门应该包含在缓存中")
            assertTrue(deptIdsAfter.size >= beforeSize, "部门数量应该增加或保持不变")
        } finally {
            // 清理测试数据
            authDeptDao.deleteById(id)
            cacheHandler.syncOnDelete(authDept, id)
        }
    }

    @Test
    fun syncOnUpdate() {
        val tenantId = "tenant-001"
        val authDept = AuthDept().apply {
            this.tenantId = tenantId
            this.name = "测试部门_${System.currentTimeMillis()}"
            this.deptTypeDictCode = "DEPT_TYPE_DEFAULT"
            this.active = true
        }
        val id = authDeptDao.insert(authDept)

        try {
            // 先同步插入缓存
            cacheHandler.syncOnInsert(authDept, id)
            
            // 先获取一次，确保缓存中有数据
            val deptIdsBefore = cacheHandler.getDeptIds(tenantId)
            assertTrue(deptIdsBefore.contains(id), "新部门应该在缓存中")

            // 更新数据库记录
            val success = authDeptDao.updateProperties(id, mapOf(AuthDept::name.name to "更新后的部门名"))
            assert(success)

            // 同步缓存
            cacheHandler.syncOnUpdate(null, id)

            // 验证缓存已被清除并重新加载
            val deptIdsAfter = cacheHandler.getDeptIds(tenantId)
            assertTrue(deptIdsAfter.contains(id), "更新后的部门应该仍在缓存中")
        } finally {
            // 清理测试数据
            authDeptDao.deleteById(id)
            cacheHandler.syncOnDelete(authDept, id)
        }
    }

    @Test
    fun syncOnUpdateActive() {
        val tenantId = "tenant-001"
        val authDept = AuthDept().apply {
            this.tenantId = tenantId
            this.name = "测试部门_${System.currentTimeMillis()}"
            this.deptTypeDictCode = "DEPT_TYPE_DEFAULT"
            this.active = false
        }
        val id = authDeptDao.insert(authDept)

        try {
            // 由false更新为true
            val success = authDeptDao.updateProperties(id, mapOf(AuthDept::active.name to true))
            assert(success)
            cacheHandler.syncOnUpdateActive(id, true)
            var deptIds = cacheHandler.getDeptIds(tenantId)
            assertTrue(deptIds.contains(id), "激活后的部门应该包含在缓存中")

            // 由true更新为false
            authDeptDao.updateProperties(id, mapOf(AuthDept::active.name to false))
            cacheHandler.syncOnUpdateActive(id, false)
            deptIds = cacheHandler.getDeptIds(tenantId)
            assertTrue(!deptIds.contains(id), "停用后的部门不应该包含在缓存中")
        } finally {
            // 清理测试数据
            authDeptDao.deleteById(id)
            cacheHandler.syncOnDelete(authDept, id)
        }
    }

    @Test
    fun syncOnDelete() {
        val tenantId = "tenant-001"
        val authDept = AuthDept().apply {
            this.tenantId = tenantId
            this.name = "测试部门_${System.currentTimeMillis()}"
            this.deptTypeDictCode = "DEPT_TYPE_DEFAULT"
            this.active = true
        }
        val id = authDeptDao.insert(authDept)

        // 先同步插入缓存
        cacheHandler.syncOnInsert(authDept, id)
        
        // 先获取一次，确保缓存中有数据
        val deptIdsBefore = cacheHandler.getDeptIds(tenantId)
        assertTrue(deptIdsBefore.contains(id), "新部门应该在缓存中")

        // 删除数据库记录
        val deleteSuccess = authDeptDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(authDept, id)

        // 验证缓存已被清除并重新加载
        val deptIdsAfter = cacheHandler.getDeptIds(tenantId)
        assertTrue(!deptIdsAfter.contains(id), "删除后的部门不应该包含在缓存中")
    }

    @Test
    fun syncOnBatchDelete() {
        val tenantId = "tenant-001"
        
        val authDept1 = AuthDept().apply {
            this.tenantId = tenantId
            this.name = "测试部门1_${System.currentTimeMillis()}"
            this.deptTypeDictCode = "DEPT_TYPE_DEFAULT"
            this.active = true
        }
        val id1 = authDeptDao.insert(authDept1)
        
        val authDept2 = AuthDept().apply {
            this.tenantId = tenantId
            this.name = "测试部门2_${System.currentTimeMillis()}"
            this.deptTypeDictCode = "DEPT_TYPE_DEFAULT"
            this.active = true
        }
        val id2 = authDeptDao.insert(authDept2)

        // 先同步插入缓存
        cacheHandler.syncOnInsert(authDept1, id1)
        cacheHandler.syncOnInsert(authDept2, id2)

        // 先获取一次，确保缓存中有数据
        val deptIdsBefore = cacheHandler.getDeptIds(tenantId)
        assertTrue(deptIdsBefore.contains(id1), "新部门1应该在缓存中")
        assertTrue(deptIdsBefore.contains(id2), "新部门2应该在缓存中")

        // 批量删除数据库记录
        val ids = listOf(id1, id2)
        val count = authDeptDao.batchDelete(ids)
        assert(count == 2)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(ids, listOf(tenantId, tenantId))

        // 验证缓存已被清除并重新加载
        val deptIdsAfter = cacheHandler.getDeptIds(tenantId)
        assertTrue(!deptIdsAfter.contains(id1), "删除后的部门1不应该包含在缓存中")
        assertTrue(!deptIdsAfter.contains(id2), "删除后的部门2不应该包含在缓存中")
    }

}
