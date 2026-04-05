package io.kudos.ms.sys.core.tenant.service
import io.kudos.ms.sys.common.tenant.vo.request.SysTenantFormCreate
import io.kudos.ms.sys.common.tenant.vo.SysTenantCacheEntry
import io.kudos.ms.sys.common.tenant.vo.response.SysTenantDetail
import io.kudos.ms.sys.core.tenant.cache.SysTenantSystemHashCache
import io.kudos.ms.sys.core.tenant.cache.TenantByIdCache
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysTenantService
 *
 * 测试数据来源：`SysTenantServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysTenantService: ISysTenantService

    @Resource
    private lateinit var tenantByIdCache: TenantByIdCache

    @Resource
    private lateinit var sysTenantSystemHashCache: SysTenantSystemHashCache

    private val seededTenantId = "20000000-0000-0000-0000-000000006144"
    private val seededSubSystemCode = "svc-subsys-tenant-test-1_2492"

    /** 按主键调用 `get(id)` 能取到租户 PO，且 `id` 与主键列一致。 */
    @Test
    fun get_byId_primaryKey_entityIdEqualsId() {
        val row = sysTenantService.get(seededTenantId)
        assertNotNull(row)
        assertEquals(seededTenantId, row.id)
    }

    /** `get(id, SysTenantCacheEntry::class)` 与 `getTenantFromCache(id)` 结果一致；走 by-id 缓存前可 `reloadAll` 与测试 SQL 对齐。 */
    @Test
    fun get_withCacheEntryReturnType_delegatesToTenantByIdCache() {
        tenantByIdCache.reloadAll(clear = true)
        val fromGet = sysTenantService.get(seededTenantId, SysTenantCacheEntry::class)
        val fromCache = sysTenantService.getTenantFromCache(seededTenantId)
        assertNotNull(fromGet)
        assertNotNull(fromCache)
        assertEquals(fromCache.id, fromGet.id)
        assertEquals(seededTenantId, fromGet.id)
    }

    /** 按主键从缓存读取 [SysTenantCacheEntry]，校验 id、name。 */
    @Test
    fun getTenantFromCache_byId() {
        tenantByIdCache.reloadAll(clear = true)
        val entry = sysTenantService.getTenantFromCache(seededTenantId)
        assertNotNull(entry)
        assertEquals(seededTenantId, entry.id)
        assertEquals("svc-tenant-test-1", entry.name)
    }

    /** `getTenantsFromCacheByIds` 对种子 id 返回非空映射，且与单条缓存一致。 */
    @Test
    fun getTenantsFromCacheByIds() {
        tenantByIdCache.reloadAll(clear = true)
        val map = sysTenantService.getTenantsFromCacheByIds(listOf(seededTenantId))
        assertEquals(1, map.size)
        assertEquals(seededTenantId, map[seededTenantId]?.id)
    }

    /** 按子系统编码从 Hash 缓存解析租户列表（含未启用语义由调用方过滤；此处校验能命中种子租户）。 */
    @Test
    fun getTenantsForSubSystemFromCache() {
        sysTenantSystemHashCache.reloadAll(clear = true)
        tenantByIdCache.reloadAll(clear = true)
        val list = sysTenantService.getTenantsForSubSystemFromCache(seededSubSystemCode)
        assertTrue(list.any { it.id == seededTenantId })
    }

    /** `getAllTenantsFromCache` 以库为源加载缓存载体列表，应包含种子租户。 */
    @Test
    fun getAllTenantsFromCache_containsSeeded() {
        val all = sysTenantService.getAllTenantsFromCache()
        assertTrue(all.any { it.id == seededTenantId })
    }

    /** 详情 [SysTenantDetail] 在 Service 层补充 `subSystemCodes`（逗号拼接子系统编码）。 */
    @Test
    fun getDetail_subSystemCodesPopulated() {
        sysTenantSystemHashCache.reloadAll(clear = true)
        val detail = sysTenantService.get(seededTenantId, SysTenantDetail::class)
        assertNotNull(detail)
        assertTrue(detail.subSystemCodes.contains(seededSubSystemCode))
    }

    /** 按 id 取列表行与按名称查询，与种子数据一致。 */
    @Test
    fun getTenantRecord_and_getTenantByName() {
        val record = sysTenantService.getTenantRecord(seededTenantId)
        assertNotNull(record)
        assertEquals(seededTenantId, record.id)

        val byName = sysTenantService.getTenantByName("svc-tenant-test-1")
        assertNotNull(byName)
        assertEquals(seededTenantId, byName.id)
    }

    /** 从租户-系统 Hash 缓存取该租户绑定的子系统编码集合。 */
    @Test
    fun getSubSystemCodesFromCache() {
        sysTenantSystemHashCache.reloadAll(clear = true)
        val codes = sysTenantService.getSubSystemCodesFromCache(seededTenantId)
        assertTrue(codes.contains(seededSubSystemCode))
    }

    /** 启用状态更新成功并可写回 true（验证写库 + 缓存同步链路）。 */
    @Test
    fun updateActive() {
        assertTrue(sysTenantService.updateActive(seededTenantId, false))
        assertTrue(sysTenantService.updateActive(seededTenantId, true))
    }

    /** 删除不存在的主键时返回 false（先查库再删）。 */
    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysTenantService.deleteById("00000000-0000-0000-0000-000000000001"))
    }

    /** 没有关联任何系统关系的租户也应允许删除。 */
    @Test
    fun deleteById_succeedsWhenTenantHasNoSystemRelations() {
        val tenantId = sysTenantService.insert(
            SysTenantFormCreate(
                name = "svc-tenant-without-system",
                subSystemCodes = emptySet(),
                timezone = null,
                defaultLanguageCode = null,
                remark = null,
            )
        )

        assertTrue(sysTenantService.deleteById(tenantId))
        assertEquals(null, sysTenantService.getTenantFromCache(tenantId))
    }
}
