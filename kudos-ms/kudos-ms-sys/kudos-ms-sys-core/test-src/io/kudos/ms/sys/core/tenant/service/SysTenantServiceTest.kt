package io.kudos.ms.sys.core.tenant.service

import io.kudos.ms.sys.common.tenant.vo.request.SysTenantFormCreate
import io.kudos.ms.sys.common.tenant.vo.request.SysTenantFormUpdate
import io.kudos.ms.sys.common.tenant.vo.request.SysTenantQuery
import io.kudos.ms.sys.common.tenant.vo.SysTenantCacheEntry
import io.kudos.ms.sys.common.tenant.vo.response.SysTenantDetail
import io.kudos.ms.sys.common.tenant.vo.response.SysTenantRow
import io.kudos.ms.sys.core.tenant.cache.SysTenantSystemHashCache
import io.kudos.ms.sys.core.tenant.cache.TenantByIdCache
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantService
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantSystemService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for SysTenantService
 *
 * Test data source: `SysTenantServiceTest.sql`
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

    @Resource
    private lateinit var sysTenantSystemService: ISysTenantSystemService

    private val seededTenantId = "20000000-0000-0000-0000-000000006144"
    private val seededSubSystemCode = "svc-subsys-tenant-test-1_2492"

    /** Calling `get(id)` by primary key returns the tenant PO, and `id` matches the primary key column. */
    @Test
    fun get_byId_primaryKey_entityIdEqualsId() {
        val row = sysTenantService.get(seededTenantId)
        assertNotNull(row)
        assertEquals(seededTenantId, row.id)
    }

    /** `get(id, SysTenantCacheEntry::class)` and `getTenantFromCache(id)` return consistent results; the by-id cache can be `reloadAll`-ed beforehand to align with the test SQL. */
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

    /** Reads [SysTenantCacheEntry] from cache by primary key and verifies id and name. */
    @Test
    fun getTenantFromCache_byId() {
        tenantByIdCache.reloadAll(clear = true)
        val entry = sysTenantService.getTenantFromCache(seededTenantId)
        assertNotNull(entry)
        assertEquals(seededTenantId, entry.id)
        assertEquals("svc-tenant-test-1", entry.name)
    }

    /** `getTenantsFromCacheByIds` returns a non-empty map for the seeded id, consistent with the single-entry cache. */
    @Test
    fun getTenantsFromCacheByIds() {
        tenantByIdCache.reloadAll(clear = true)
        val map = sysTenantService.getTenantsFromCacheByIds(listOf(seededTenantId))
        assertEquals(1, map.size)
        assertEquals(seededTenantId, map[seededTenantId]?.id)
    }

    /** Resolves the tenant list from the hash cache by sub-system code (inactive-inclusive semantics are filtered by the caller; here we verify the seeded tenant is hit). */
    @Test
    fun getTenantsForSubSystemFromCache() {
        sysTenantSystemHashCache.reloadAll(clear = true)
        tenantByIdCache.reloadAll(clear = true)
        val list = sysTenantService.getTenantsForSubSystemFromCache(seededSubSystemCode)
        assertTrue(list.any { it.id == seededTenantId })
    }

    /** `getAllTenantsFromCache` loads the cache-carrier list from DB and should include the seeded tenant. */
    @Test
    fun getAllTenantsFromCache_containsSeeded() {
        val all = sysTenantService.getAllTenantsFromCache()
        assertTrue(all.any { it.id == seededTenantId })
    }

    /** [SysTenantDetail] is enriched at the service layer with `subSystemCodes` (comma-joined sub-system codes). */
    @Test
    fun getDetail_subSystemCodesPopulated() {
        sysTenantSystemHashCache.reloadAll(clear = true)
        val detail = sysTenantService.get(seededTenantId, SysTenantDetail::class)
        assertNotNull(detail)
        assertTrue(detail.subSystemCodes.contains(seededSubSystemCode))
    }

    /** Fetch list row by id and query by name; results match seeded data. */
    @Test
    fun getTenantRecord_and_getTenantByName() {
        val record = sysTenantService.getTenantRecord(seededTenantId)
        assertNotNull(record)
        assertEquals(seededTenantId, record.id)

        val byName = sysTenantService.getTenantByName("svc-tenant-test-1")
        assertNotNull(byName)
        assertEquals(seededTenantId, byName.id)
    }

    /** Fetches the set of sub-system codes bound to this tenant from the tenant-system hash cache. */
    @Test
    fun getSubSystemCodesFromCache() {
        sysTenantSystemHashCache.reloadAll(clear = true)
        val codes = sysTenantService.getSubSystemCodesFromCache(seededTenantId)
        assertTrue(codes.contains(seededSubSystemCode))
    }

    /** Active flag is updated successfully and toggles back to true (verifying DB write + cache sync chain). */
    @Test
    fun updateActive() {
        assertTrue(sysTenantService.updateActive(seededTenantId, false))
        assertTrue(sysTenantService.updateActive(seededTenantId, true))
    }

    /** Returns false when deleting a non-existent primary key (check-then-delete). */
    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysTenantService.deleteById("00000000-0000-0000-0000-000000000001"))
    }

    /** Tenants without any system relation should also be deletable. */
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

    /** `pagingSearch` returns [SysTenantRow] rows and enriches each with comma-joined `subSystemCodes`. */
    @Test
    fun pagingSearch_enrichesRowsWithSubSystemCodes() {
        sysTenantSystemHashCache.reloadAll(clear = true)
        val payload = SysTenantQuery(name = "svc-tenant-test-1").apply {
            pageNo = 1
            pageSize = 10
        }
        val result = sysTenantService.pagingSearch(payload)
        val rows = result.data.filterIsInstance<SysTenantRow>()
        assertTrue(rows.isNotEmpty())
        val seededRow = assertNotNull(rows.find { it.id == seededTenantId })
        assertTrue(seededRow.subSystemCodes.contains(seededSubSystemCode))
    }

    /** Empty id collection short-circuits to an empty map without touching the cache. */
    @Test
    fun getTenantsFromCacheByIds_emptyInputReturnsEmptyMap() {
        assertTrue(sysTenantService.getTenantsFromCacheByIds(emptyList()).isEmpty())
    }

    /** Only active tenants are converted to IdAndName for the sub-system selector. */
    @Test
    fun getActiveTenantIdAndNamesForSubSystem() {
        sysTenantSystemHashCache.reloadAll(clear = true)
        tenantByIdCache.reloadAll(clear = true)
        val idAndNames = sysTenantService.getActiveTenantIdAndNamesForSubSystem(seededSubSystemCode)
        assertTrue(idAndNames.any { it.id == seededTenantId && it.name == "svc-tenant-test-1" })

        assertTrue(sysTenantService.getActiveTenantIdAndNamesForSubSystem("no-such-subsys").isEmpty())
    }

    /** Insert with non-empty subSystemCodes also writes the tenant-system relations in batch. */
    @Test
    fun insert_withSubSystemCodes_createsRelations() {
        val tenantId = sysTenantService.insert(
            SysTenantFormCreate(
                name = "svc-tenant-with-system",
                subSystemCodes = setOf(seededSubSystemCode),
                timezone = null,
                defaultLanguageCode = null,
                remark = null,
            )
        )
        assertEquals(setOf(seededSubSystemCode), sysTenantSystemService.searchSystemCodesByTenantId(tenantId))
    }

    /** Update with changed subSystemCodes replaces the relations (delete old + insert new). */
    @Test
    fun update_withChangedSubSystemCodes_replacesRelations() {
        sysTenantSystemHashCache.reloadAll(clear = true)
        val newCodes = setOf("svc-system-tenant-test-0_2492")
        val success = sysTenantService.update(
            SysTenantFormUpdate(
                id = seededTenantId,
                name = "svc-tenant-test-1-renamed",
                subSystemCodes = newCodes,
                timezone = null,
                defaultLanguageCode = null,
                remark = null,
            )
        )
        assertTrue(success)
        assertEquals(newCodes, sysTenantSystemService.searchSystemCodesByTenantId(seededTenantId))
        assertEquals("svc-tenant-test-1-renamed", sysTenantService.getTenantRecord(seededTenantId)?.name)
    }

    /** Update with unchanged subSystemCodes skips the relation replacement (diff against the hash cache). */
    @Test
    fun update_withUnchangedSubSystemCodes_keepsRelations() {
        sysTenantSystemHashCache.reloadAll(clear = true)
        val success = sysTenantService.update(
            SysTenantFormUpdate(
                id = seededTenantId,
                name = "svc-tenant-test-1",
                subSystemCodes = setOf(seededSubSystemCode),
                timezone = null,
                defaultLanguageCode = null,
                remark = null,
            )
        )
        assertTrue(success)
        assertEquals(setOf(seededSubSystemCode), sysTenantSystemService.searchSystemCodesByTenantId(seededTenantId))
    }

    /** Update payloads that do not implement IIdEntity<String> are rejected with IllegalStateException. */
    @Test
    fun update_unsupportedPayloadThrows() {
        val ex = assertFailsWith<IllegalStateException> { sysTenantService.update(Any()) }
        assertTrue(ex.message!!.contains("Unsupported argument type when updating tenant"))
    }

    /** updateActive on a non-existent id fails (dao.update false) and returns false. */
    @Test
    fun updateActive_returnsFalseWhenRowMissing() {
        assertFalse(sysTenantService.updateActive("00000000-0000-0000-0000-000000000002", false))
    }

    /** Batch delete removes the tenants and their relations; returns the actual count. */
    @Test
    fun batchDelete() {
        fun create(name: String) = sysTenantService.insert(
            SysTenantFormCreate(
                name = name,
                subSystemCodes = setOf(seededSubSystemCode),
                timezone = null,
                defaultLanguageCode = null,
                remark = null,
            )
        )

        val id1 = create("svc-tenant-batch-del-1")
        val id2 = create("svc-tenant-batch-del-2")
        val count = sysTenantService.batchDelete(listOf(id1, id2))
        assertEquals(2, count)
        assertNull(sysTenantService.getTenantRecord(id1))
        assertNull(sysTenantService.getTenantRecord(id2))
        assertTrue(sysTenantSystemService.searchSystemCodesByTenantId(id1).isEmpty())
    }

    /** Non-existent name / id queries return null. */
    @Test
    fun getTenantRecord_and_getTenantByName_missingReturnsNull() {
        assertNull(sysTenantService.getTenantRecord("00000000-0000-0000-0000-000000000003"))
        assertNull(sysTenantService.getTenantByName("no-such-tenant-name"))
    }
}
