package io.kudos.ams.sys.core.cache

import io.kudos.ams.sys.core.dao.SysTenantSystemDao
import io.kudos.ams.sys.core.model.po.SysTenantSystem
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse


/**
 * junit test for TenantIdsBySystemCodeCacheHandler
 *
 * 测试数据来源：`TenantIdsBySystemCodeCacheHandlerTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class TenantIdsBySystemCodeCacheHandlerTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: TenantIdsBySystemCodeCacheHandler

    @Resource
    private lateinit var dao: SysTenantSystemDao

    @Test
    fun reloadAll() {
        // 清除并重载缓存，保证与数据库中的数据一致
        cacheHandler.reloadAll(true)

        // 插入新的记录到数据库
        val sysResourceNew = insertNewRecordToDb()

        // 从数据库中删除记录
        val idDelete = "b3846388-5e61-4b58-8fd8-bbbbbbbbbbbb"
        dao.deleteById(idDelete)

        // 重载缓存，但不清除旧缓存
        cacheHandler.reloadAll(false)

        // 数据库中新增的记录在缓存应该要存在
        var tenantIds = cacheHandler.getTenantIds(sysResourceNew.systemCode)
        assert(tenantIds.contains(sysResourceNew.tenantId))

        // 数据库中删除的记录在缓存中应该不存在
        val systemCode = "subSys-a"
        tenantIds = cacheHandler.getTenantIds(systemCode)
        assertFalse(tenantIds.contains(idDelete))
    }

    @Test
    fun getTenantIds() {
        // 存在的
        var systemCode = "subSys-a"
        assert(cacheHandler.getTenantIds(systemCode).isNotEmpty())

        // 不存在的
        systemCode = "subSys-nnn"
        assert(cacheHandler.getTenantIds(systemCode).isEmpty())
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val sysTenantSystem = insertNewRecordToDb()

        // 同步缓存
        cacheHandler.syncOnInsert(sysTenantSystem, sysTenantSystem.id!!)

        // 验证新记录是否在缓存中
        val tenantIds = cacheHandler.getTenantIds(sysTenantSystem.systemCode)
        assert(tenantIds.contains(sysTenantSystem.tenantId))
    }

    @Test
    fun syncOnDelete() {
        val id = "b3846388-5e61-4b58-8fd8-eeeeeeeeeeee"
        val sysTenantSystem = dao.get(id)!!
        val tenantId = sysTenantSystem.tenantId
        val systemCodes = dao.searchSystemCodesByTenantId(tenantId)

        // 删除数据库中的记录
        val deleteSuccess = dao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(tenantId, systemCodes)

        // 验证缓存中有没有
        val tenantIds = cacheHandler.getTenantIds(sysTenantSystem.systemCode)
        assertFalse(tenantIds.contains(tenantId))
    }

    @Test
    fun syncOnBatchDelete() {
        val id1 = "b3846388-5e61-4b58-8fd8-ffffffffffff"
        val id2 = "b3846388-5e61-4b58-8fd8-gggggggggggg"
        val ids = listOf(id1, id2)
        val systemCode1 = "subSys-a"
        val systemCode2 = "subSys-d"
        val systemCodes = listOf(systemCode1, systemCode2)
        val tenantId1 = "118772a0-c053-4634-a5e5-444444444444"
        val tenantId2 = "118772a0-c053-4634-a5e5-555555555555"

        // 批量删除数据库中的记录
        val count = dao.batchDelete(ids)
        assertEquals(2, count)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(ids, systemCodes)

        // 验证缓存中有没有
        assertFalse(cacheHandler.getTenantIds(systemCode1).contains(tenantId1))
        assertFalse(cacheHandler.getTenantIds(systemCode2).contains(tenantId2))
    }

    private fun insertNewRecordToDb(): SysTenantSystem {
        val sysTenantSystem = SysTenantSystem().apply {
            systemCode = "subSys-a"
            tenantId = "tenantId-n"
        }
        dao.insert(sysTenantSystem)
        return sysTenantSystem
    }

}
