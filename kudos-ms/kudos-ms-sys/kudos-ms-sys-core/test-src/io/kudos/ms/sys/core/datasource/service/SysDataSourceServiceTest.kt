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
 * Test data source: `SysDataSourceServiceTest.sql`
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

    /** Calling `get(id)` by primary key returns the PO, and `id` matches the primary key column. */
    @Test
    fun get_byId_primaryKey_entityIdEqualsId() {
        val row = sysDataSourceService.get(seededDataSourceId)
        assertNotNull(row)
        assertEquals(seededDataSourceId, row.id)
    }

    /** `get(id, SysDataSourceCacheEntry::class)` and `getDataSourceFromCache(id)` return consistent results; the hash cache must be `reloadAll`-ed beforehand to align with the test SQL. */
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

    /** Reads [SysDataSourceCacheEntry] from cache by primary key and verifies id and tenantId. */
    @Test
    fun getDataSourceFromCache_byId() {
        sysDataSourceHashCache.reloadAll(clear = true)
        val entry = sysDataSourceService.getDataSourceFromCache(seededDataSourceId)
        assertNotNull(entry)
        assertEquals(seededDataSourceId, entry.id)
        assertEquals(seededTenantId, entry.tenantId)
    }

    /** Querying the cache list by tenant + sub-system + microservice should include the seeded data source. */
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

    /** Two-arg `getDataSourceFromCache(tenantId, microServiceCode)` (internal subSystem=null) hits the seeded row. */
    @Test
    fun getDataSourceFromCache_byTenantAndAtomicMicroServiceCode() {
        sysDataSourceHashCache.reloadAll(clear = true)
        val entry = sysDataSourceService.getDataSourceFromCache(seededTenantId, seededMicroServiceCode)
        assertNotNull(entry)
        assertEquals(seededDataSourceId, entry.id)
    }

    /** [SysDataSourceDetail] is enriched at the service layer with `tenantName` (from tenant API). */
    @Test
    fun getDetail_tenantNamePopulated() {
        val detail = sysDataSourceService.get(seededDataSourceId, SysDataSourceDetail::class)
        assertNotNull(detail)
        assertEquals(seededTenantId, detail.tenantId)
        assertEquals("svc-tenant-ds-test-1", detail.tenantName)
    }

    /** Conditional DAO query of [SysDataSourceRow] by tenant id should include the seeded row. */
    @Test
    fun getDataSourcesByTenantId() {
        val dataSources = sysDataSourceService.getDataSourcesByTenantId(seededTenantId)
        assertTrue(dataSources.any { it.id == seededDataSourceId })
    }

    /** Conditional DAO query by sub-system code. */
    @Test
    fun getDataSourcesBySubSystemCode() {
        val dataSources = sysDataSourceService.getDataSourcesBySubSystemCode(seededSubSystemCode)
        assertTrue(dataSources.any { it.subSystemCode == seededSubSystemCode })
    }

    /** Active flag is updated successfully and toggles back to true (verifying DB write + cache sync chain). */
    @Test
    fun updateActive() {
        assertTrue(sysDataSourceService.updateActive(seededDataSourceId, false))
        assertTrue(sysDataSourceService.updateActive(seededDataSourceId, true))
    }

    /** Returns false when deleting a non-existent primary key (consistent with the check-then-delete implementation). */
    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysDataSourceService.deleteById("00000000-0000-0000-0000-000000000001"))
    }

    /** After inserting a unique (tenant, subSystem, microService) row and deleting by id, it can no longer be queried. */
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

    /** Batch deletes two newly inserted records, returns delete count 2, and neither key can be queried again. */
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
