package io.kudos.ams.auth.provider.cache

import io.kudos.ability.cache.common.enums.CacheStrategy
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
class DeptIdsByTenantIdCacheHandlerTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var cacheHandler: DeptIdsByTenantIdCacheHandler

    override fun getCacheStrategy(): String {
        return CacheStrategy.LOCAL_REMOTE.name
    }

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

}
