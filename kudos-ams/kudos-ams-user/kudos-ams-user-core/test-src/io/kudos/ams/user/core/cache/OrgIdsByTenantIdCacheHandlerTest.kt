package io.kudos.ams.user.core.cache

import io.kudos.ams.user.core.dao.UserOrgDao
import io.kudos.ams.user.core.model.po.UserOrg
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for OrgIdsByTenantIdCacheHandler
 *
 * 测试数据来源：`OrgIdsByTenantIdCacheHandlerTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class OrgIdsByTenantIdCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: OrgIdsByTenantIdCacheHandler

    @Resource
    private lateinit var userOrgDao: UserOrgDao

    @Test
    fun getOrgIds() {
        // 存在的租户（有多个机构）- 使用 OrgIdsByTenantIdCacheHandlerTest.sql 的测试数据
        var tenantId = "tenant-001-lVeGsiPZ"
        val orgIds1 = cacheHandler.getOrgIds(tenantId)
        val orgIds2 = cacheHandler.getOrgIds(tenantId)
        assertEquals(
            7,
            orgIds1.size,
            "tenant-001 应该有 7 个 active=true 的机构（技术部、产品部、运营部、前端组、后端组、测试组、人事部）"
        )
        assertTrue(orgIds1.contains("4637af03-1111-1111-1111-111111111111"), "应该包含技术部ID") // 技术部
        assertTrue(orgIds1.contains("4637af03-2222-2222-2222-222222222222"), "应该包含产品部ID") // 产品部
        assertTrue(orgIds1.contains("4637af03-8888-8888-8888-888888888888"), "应该包含人事部ID") // 人事部
        assertEquals(orgIds1, orgIds2, "两次调用应该返回相同的结果（缓存验证）")

        // 存在的租户（只有 1 个机构）
        tenantId = "tenant-002-lVeGsiPZ"
        val orgIds3 = cacheHandler.getOrgIds(tenantId)
        assertEquals(1, orgIds3.size, "tenant-002 应该有 1 个 active=true 的机构（总部）")
        assertTrue(orgIds3.contains("4637af03-9999-9999-9999-999999999999"), "应该包含总部ID") // 总部

        // 不存在的租户
        tenantId = "no_exist_tenant"
        val orgIds4 = cacheHandler.getOrgIds(tenantId)
        assertTrue(orgIds4.isEmpty(), "不存在的租户应该返回空列表")
    }

    @Test
    fun syncOnInsert() {
        val tenantId = "tenant-001-lVeGsiPZ"
        val userOrg = UserOrg().apply {
            this.tenantId = tenantId
            this.name = "测试机构_${System.currentTimeMillis()}"
            this.orgTypeDictCode = "ORG_TYPE_DEFAULT"
            this.active = true
        }
        val id = userOrgDao.insert(userOrg)

        try {
            // 先获取一次，记录机构数量
            val orgIdsBefore = cacheHandler.getOrgIds(tenantId)
            val beforeSize = orgIdsBefore.size

            // 同步缓存
            cacheHandler.syncOnInsert(userOrg, id)

            // 验证新记录是否在缓存中
            val orgIdsAfter = cacheHandler.getOrgIds(tenantId)
            assertTrue(orgIdsAfter.contains(id), "新机构应该包含在缓存中")
            assertTrue(orgIdsAfter.size >= beforeSize, "机构数量应该增加或保持不变")
        } finally {
            // 清理测试数据
            userOrgDao.deleteById(id)
            cacheHandler.syncOnDelete(userOrg, id)
        }
    }

    @Test
    fun syncOnUpdate() {
        val tenantId = "tenant-001-lVeGsiPZ"
        val org = UserOrg().apply {
            this.tenantId = tenantId
            this.name = "测试机构_${System.currentTimeMillis()}"
            this.orgTypeDictCode = "ORG_TYPE_DEFAULT"
            this.active = true
        }
        val id = userOrgDao.insert(org)

        try {
            // 先同步插入缓存
            cacheHandler.syncOnInsert(org, id)
            
            // 先获取一次，确保缓存中有数据
            val orgIdsBefore = cacheHandler.getOrgIds(tenantId)
            assertTrue(orgIdsBefore.contains(id), "新机构应该在缓存中")

            // 更新数据库记录
            val success = userOrgDao.updateProperties(id, mapOf(UserOrg::name.name to "更新后的机构名"))
            assert(success)

            // 同步缓存
            cacheHandler.syncOnUpdate(null, id)

            // 验证缓存已被清除并重新加载
            val orgIdsAfter = cacheHandler.getOrgIds(tenantId)
            assertTrue(orgIdsAfter.contains(id), "更新后的机构应该仍在缓存中")
        } finally {
            // 清理测试数据
            userOrgDao.deleteById(id)
            cacheHandler.syncOnDelete(org, id)
        }
    }

    @Test
    fun syncOnUpdateActive() {
        val tenantId = "tenant-001-lVeGsiPZ"
        val org = UserOrg().apply {
            this.tenantId = tenantId
            this.name = "测试机构_${System.currentTimeMillis()}"
            this.orgTypeDictCode = "ORG_TYPE_DEFAULT"
            this.active = false
        }
        val id = userOrgDao.insert(org)

        try {
            // 由false更新为true
            val success = userOrgDao.updateProperties(id, mapOf(UserOrg::active.name to true))
            assert(success)
            cacheHandler.syncOnUpdateActive(id, true)
            var orgIds = cacheHandler.getOrgIds(tenantId)
            assertTrue(orgIds.contains(id), "激活后的机构应该包含在缓存中")

            // 由true更新为false
            userOrgDao.updateProperties(id, mapOf(UserOrg::active.name to false))
            cacheHandler.syncOnUpdateActive(id, false)
            orgIds = cacheHandler.getOrgIds(tenantId)
            assertTrue(!orgIds.contains(id), "停用后的机构不应该包含在缓存中")
        } finally {
            // 清理测试数据
            userOrgDao.deleteById(id)
            cacheHandler.syncOnDelete(org, id)
        }
    }

    @Test
    fun syncOnDelete() {
        val tenantId = "tenant-001-lVeGsiPZ"
        val userOrg = UserOrg().apply {
            this.tenantId = tenantId
            this.name = "测试机构_${System.currentTimeMillis()}"
            this.orgTypeDictCode = "ORG_TYPE_DEFAULT"
            this.active = true
        }
        val id = userOrgDao.insert(userOrg)

        // 先同步插入缓存
        cacheHandler.syncOnInsert(userOrg, id)
        
        // 先获取一次，确保缓存中有数据
        val orgIdsBefore = cacheHandler.getOrgIds(tenantId)
        assertTrue(orgIdsBefore.contains(id), "新机构应该在缓存中")

        // 删除数据库记录
        val deleteSuccess = userOrgDao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(userOrg, id)

        // 验证缓存已被清除并重新加载
        val orgIdsAfter = cacheHandler.getOrgIds(tenantId)
        assertTrue(!orgIdsAfter.contains(id), "删除后的机构不应该包含在缓存中")
    }

    @Test
    fun syncOnBatchDelete() {
        val tenantId = "tenant-001-lVeGsiPZ"
        
        val userOrg1 = UserOrg().apply {
            this.tenantId = tenantId
            this.name = "测试机构1_${System.currentTimeMillis()}"
            this.orgTypeDictCode = "ORG_TYPE_DEFAULT"
            this.active = true
        }
        val id1 = userOrgDao.insert(userOrg1)
        
        val userOrg2 = UserOrg().apply {
            this.tenantId = tenantId
            this.name = "测试机构2_${System.currentTimeMillis()}"
            this.orgTypeDictCode = "ORG_TYPE_DEFAULT"
            this.active = true
        }
        val id2 = userOrgDao.insert(userOrg2)

        // 先同步插入缓存
        cacheHandler.syncOnInsert(userOrg1, id1)
        cacheHandler.syncOnInsert(userOrg2, id2)

        // 先获取一次，确保缓存中有数据
        val orgIdsBefore = cacheHandler.getOrgIds(tenantId)
        assertTrue(orgIdsBefore.contains(id1), "新机构1应该在缓存中")
        assertTrue(orgIdsBefore.contains(id2), "新机构2应该在缓存中")

        // 批量删除数据库记录
        val ids = listOf(id1, id2)
        val count = userOrgDao.batchDelete(ids)
        assert(count == 2)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(ids, listOf(tenantId, tenantId))

        // 验证缓存已被清除并重新加载
        val orgIdsAfter = cacheHandler.getOrgIds(tenantId)
        assertTrue(!orgIdsAfter.contains(id1), "删除后的机构1不应该包含在缓存中")
        assertTrue(!orgIdsAfter.contains(id2), "删除后的机构2不应该包含在缓存中")
    }

}
