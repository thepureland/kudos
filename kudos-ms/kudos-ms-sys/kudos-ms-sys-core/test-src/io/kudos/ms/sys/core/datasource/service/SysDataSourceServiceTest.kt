package io.kudos.ms.sys.core.datasource.service

import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import io.kudos.ms.sys.common.datasource.vo.response.SysDataSourceDetail
import io.kudos.ms.sys.core.datasource.cache.SysDataSourceHashCache
import io.kudos.ms.sys.core.datasource.model.po.SysDataSource
import io.kudos.ms.sys.core.datasource.service.iservice.ISysDataSourceService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for SysDataSourceService
 *
 * 测试数据来源：`SysDataSourceServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDataSourceServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysDataSourceService: ISysDataSourceService

    @Resource
    private lateinit var sysDataSourceHashCache: SysDataSourceHashCache

    private val seededTenantId = "20000000-0000-0000-0000-000000007716"
    private val seededDataSourceId = "20000000-0000-0000-0000-000000007716"
    private val seededSubSystemCode = "svc-subsys-ds-test-1_7618"
    private val seededMicroServiceCode = "svc-ms-ds-test-1_7618"

    /** 按主键调用 `get(id)` 能取到 PO，且 `id` 与主键列一致。 */
    @Test
    fun get_byId_primaryKey_entityIdEqualsId() {
        val row = sysDataSourceService.get(seededDataSourceId)
        assertNotNull(row)
        assertEquals(seededDataSourceId, row.id)
    }

    /** `get(id, SysDataSourceCacheEntry::class)` 与 `getDataSourceFromCache(id)` 结果一致，且走 Hash 缓存前需 `reloadAll` 与测试 SQL 对齐。 */
    @Test
    fun get_withCacheEntryReturnType_delegatesToHashCache() {
        sysDataSourceHashCache.reloadAll(clear = true)
        val fromGet = sysDataSourceService.get(seededDataSourceId, SysDataSourceCacheEntry::class)
        val fromCache = sysDataSourceService.getDataSourceFromCache(seededDataSourceId)
        assertNotNull(fromGet)
        assertNotNull(fromCache)
        assertEquals(fromCache.id, fromGet.id)
        assertEquals(seededDataSourceId, fromGet.id)
    }

    /** 按主键从缓存读取 [SysDataSourceCacheEntry]，校验 id、tenantId。 */
    @Test
    fun getDataSourceFromCache_byId() {
        sysDataSourceHashCache.reloadAll(clear = true)
        val entry = sysDataSourceService.getDataSourceFromCache(seededDataSourceId)
        assertNotNull(entry)
        assertEquals(seededDataSourceId, entry.id)
        assertEquals(seededTenantId, entry.tenantId)
    }

    /** 按租户 + 子系统 + 微服务三条件从缓存查列表，应包含种子数据源。 */
    @Test
    fun getDataSourcesFromCache() {
        sysDataSourceHashCache.reloadAll(clear = true)
        val list = sysDataSourceService.getDataSourcesFromCache(
            seededTenantId,
            seededSubSystemCode,
            seededMicroServiceCode
        )
        assertTrue(list.isNotEmpty())
        assertTrue(list.any { it.id == seededDataSourceId })
    }

    /** 两参 `getDataSourceFromCache(tenantId, microServiceCode)`（内部 subSystem=null）能命中种子行。 */
    @Test
    fun getDataSourceFromCache_byTenantAndAtomicMicroServiceCode() {
        sysDataSourceHashCache.reloadAll(clear = true)
        val entry = sysDataSourceService.getDataSourceFromCache(seededTenantId, seededMicroServiceCode)
        assertNotNull(entry)
        assertEquals(seededDataSourceId, entry.id)
    }

    /** 详情 [SysDataSourceDetail] 在 Service 层补充 `tenantName`（来自租户 API）。 */
    @Test
    fun getDetail_tenantNamePopulated() {
        val detail = sysDataSourceService.get(seededDataSourceId, SysDataSourceDetail::class)
        assertNotNull(detail)
        assertEquals(seededTenantId, detail.tenantId)
        assertEquals("svc-tenant-ds-test-1", detail.tenantName)
    }

    /** 按租户 id 走 DAO 条件查询 [SysDataSourceRow]，应包含种子行。 */
    @Test
    fun getDataSourcesByTenantId() {
        val dataSources = sysDataSourceService.getDataSourcesByTenantId(seededTenantId)
        assertTrue(dataSources.any { it.id == seededDataSourceId })
    }

    /** 按子系统编码走 DAO 条件查询。 */
    @Test
    fun getDataSourcesBySubSystemCode() {
        val dataSources = sysDataSourceService.getDataSourcesBySubSystemCode(seededSubSystemCode)
        assertTrue(dataSources.any { it.subSystemCode == seededSubSystemCode })
    }

    /** 启用状态更新成功并可写回 true（验证写库 + 缓存同步链路）。 */
    @Test
    fun updateActive() {
        assertTrue(sysDataSourceService.updateActive(seededDataSourceId, false))
        assertTrue(sysDataSourceService.updateActive(seededDataSourceId, true))
    }

    /** 删除不存在的主键时返回 false（与先查再删的实现一致）。 */
    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysDataSourceService.deleteById("00000000-0000-0000-0000-000000000001"))
    }

    /** 插入唯一 (tenant, subSystem, microService) 后按 id 删除，库中不再可查。 */
    @Test
    fun deleteById_afterInsert_removesRow() {
        val unique = UUID.randomUUID().toString()
        val microCode = "tc_ms_$unique".take(32)
        val ds = SysDataSource().apply {
            name = "tc_ds_$unique"
            url = "jdbc:h2:mem:test"
            username = "sa"
            password = "sa"
            subSystemCode = seededSubSystemCode
            microServiceCode = microCode
            tenantId = seededTenantId
            active = true
            builtIn = false
        }
        val id = sysDataSourceService.insert(ds)
        assertNotNull(sysDataSourceService.get(id))
        assertTrue(sysDataSourceService.deleteById(id))
        assertNull(sysDataSourceService.get(id))
    }

    /** 批量删除两条新插入记录，返回删除条数 2 且主键均不可再查。 */
    @Test
    fun batchDelete_deletesInsertedRows() {
        val u1 = UUID.randomUUID().toString()
        val u2 = UUID.randomUUID().toString()
        val id1 = sysDataSourceService.insert(
            SysDataSource().apply {
                name = "tc_bd1_$u1"
                url = "jdbc:h2:mem:test"
                username = "sa"
                password = "sa"
                subSystemCode = seededSubSystemCode
                microServiceCode = "tc_bd_ms1_${u1.take(20)}".take(32)
                tenantId = seededTenantId
                active = true
                builtIn = false
            }
        )
        val id2 = sysDataSourceService.insert(
            SysDataSource().apply {
                name = "tc_bd2_$u2"
                url = "jdbc:h2:mem:test"
                username = "sa"
                password = "sa"
                subSystemCode = seededSubSystemCode
                microServiceCode = "tc_bd_ms2_${u2.take(20)}".take(32)
                tenantId = seededTenantId
                active = true
                builtIn = false
            }
        )
        assertEquals(2, sysDataSourceService.batchDelete(listOf(id1, id2)))
        assertNull(sysDataSourceService.get(id1))
        assertNull(sysDataSourceService.get(id2))
    }
}
