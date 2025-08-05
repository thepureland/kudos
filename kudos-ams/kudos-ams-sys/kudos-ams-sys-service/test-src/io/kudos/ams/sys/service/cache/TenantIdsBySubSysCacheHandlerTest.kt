package io.kudos.ams.sys.service.cache

import io.kudos.ams.sys.service.dao.SysTenantSubSystemDao
import io.kudos.ams.sys.service.model.po.SysTenantSubSystem
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse


/**
 * junit test for TenantIdsBySubSysCacheHandler
 *
 * @author K
 * @since 1.0.0
 */
class TenantIdsBySubSysCacheHandlerTest : CacheHandlerTestBase() {

    @Autowired
    private lateinit var cacheHandler: TenantIdsBySubSysCacheHandler

    @Autowired
    private lateinit var dao: SysTenantSubSystemDao

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
        var tenantIds = cacheHandler.getTenantIds(sysResourceNew.subSystemCode)
        assert(tenantIds.contains(sysResourceNew.tenantId))

        // 数据库中删除的记录在缓存中应该不存在
        val subSystemCode = "subSys-a"
        tenantIds = cacheHandler.getTenantIds(subSystemCode)
        assertFalse(tenantIds.contains(idDelete))
    }

    @Test
    fun getTenantIds() {
        // 存在的
        var subSystemCode = "subSys-a"
        assert(cacheHandler.getTenantIds(subSystemCode).isNotEmpty())

        // 不存在的
        subSystemCode = "subSys-nnn"
        assert(cacheHandler.getTenantIds(subSystemCode).isEmpty())
    }

    @Test
    fun syncOnInsert() {
        // 插入新的记录到数据库
        val sysTenantSubSystem = insertNewRecordToDb()

        // 同步缓存
        cacheHandler.syncOnInsert(sysTenantSubSystem, sysTenantSubSystem.id!!)

        // 验证新记录是否在缓存中
        val tenantIds = cacheHandler.getTenantIds(sysTenantSubSystem.subSystemCode)
        assert(tenantIds.contains(sysTenantSubSystem.tenantId))
    }

    @Test
    fun syncOnDelete() {
        val id = "b3846388-5e61-4b58-8fd8-eeeeeeeeeeee"
        val sysTenantSubSystem = dao.get(id)!!
        val tenantId = sysTenantSubSystem.tenantId
        val subSystemCodes = dao.searchSubSystemCodesByTenantId(tenantId)

        // 删除数据库中的记录
        val deleteSuccess = dao.deleteById(id)
        assert(deleteSuccess)

        // 同步缓存
        cacheHandler.syncOnDelete(tenantId, subSystemCodes)

        // 验证缓存中有没有
        val tenantIds = cacheHandler.getTenantIds(sysTenantSubSystem.subSystemCode)
        assertFalse(tenantIds.contains(tenantId))
    }

    @Test
    fun syncOnBatchDelete() {
        val id1 = "b3846388-5e61-4b58-8fd8-ffffffffffff"
        val id2 = "b3846388-5e61-4b58-8fd8-gggggggggggg"
        val ids = listOf(id1, id2)
        val subSystemCode1 = "subSys-a"
        val subSystemCode2 = "subSys-d"
        val subSystemCodes = listOf(subSystemCode1, subSystemCode2)
        val tenantId1 = "118772a0-c053-4634-a5e5-444444444444"
        val tenantId2 = "118772a0-c053-4634-a5e5-555555555555"

        // 批量删除数据库中的记录
        val count = dao.batchDelete(ids)
        assertEquals(2, count)

        // 同步缓存
        cacheHandler.syncOnBatchDelete(ids, subSystemCodes)

        // 验证缓存中有没有
        assertFalse(cacheHandler.getTenantIds(subSystemCode1).contains(tenantId1))
        assertFalse(cacheHandler.getTenantIds(subSystemCode2).contains(tenantId2))
    }

    private fun insertNewRecordToDb(): SysTenantSubSystem {
        val sysTenantSubSystem = SysTenantSubSystem().apply {
            subSystemCode = "subSys-a"
            tenantId = "tenantId-n"
            portalCode = "default"
        }
        dao.insert(sysTenantSubSystem)
        return sysTenantSubSystem
    }

}