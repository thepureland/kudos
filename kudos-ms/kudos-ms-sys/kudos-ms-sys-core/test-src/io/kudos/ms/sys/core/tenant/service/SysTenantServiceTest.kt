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
}
