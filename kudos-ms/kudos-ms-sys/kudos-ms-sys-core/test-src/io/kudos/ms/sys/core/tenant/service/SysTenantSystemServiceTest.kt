package io.kudos.ms.sys.core.tenant.service

import io.kudos.ms.sys.core.tenant.model.po.SysTenantSystem
import io.kudos.ms.sys.core.tenant.service.iservice.ISysTenantSystemService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysTenantSystemService
 *
 * Test data source: `SysTenantSystemServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantSystemServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysTenantSystemService: ISysTenantSystemService

    @Test
    fun searchSystemCodesByTenantId() {
        val tenantId = "20000000-0000-0000-0000-000000003675"
        val codes = sysTenantSystemService.searchSystemCodesByTenantId(tenantId)
        assertTrue(codes.contains("svc-subsys-ts-test-1_7901"))
    }

    @Test
    fun searchTenantIdsBySystemCode() {
        val systemCode = "svc-subsys-ts-test-1_7901"
        val tenantIds = sysTenantSystemService.searchTenantIdsBySystemCode(systemCode)
        assertTrue(tenantIds.contains("20000000-0000-0000-0000-000000003675"))
    }

    @Test
    fun exists() {
        val tenantId = "20000000-0000-0000-0000-000000003675"
        val systemCode = "svc-subsys-ts-test-1_7901"
        assertTrue(sysTenantSystemService.exists(tenantId, systemCode))
        assertFalse(sysTenantSystemService.exists(tenantId, "non-existent"))
    }

    @Test
    fun batchBind_and_unbind() {
        val tenantId = "20000000-0000-0000-0000-000000003675"
        val systemCode = "svc-subsys-ts-test-1_7901"
        
        // Create the new system first
        // Note: this assumes the system already exists; in real tests it may need to be created
        // For simplicity, we test unbind here
        val systemCodeToUnbind = "svc-subsys-ts-test-1_7901"
        val unbindResult = sysTenantSystemService.unbind(tenantId, systemCodeToUnbind)
        assertTrue(unbindResult)

        // Rebind
        val bindCount = sysTenantSystemService.batchBind(tenantId, listOf(systemCode))
        assertTrue(bindCount > 0)
    }

    private val tenantId = "20000000-0000-0000-0000-000000003675"
    private val subSystemCode = "svc-subsys-ts-test-1_7901"
    private val rootSystemCode = "svc-system-ts-test-0_7901"

    /** Grouping system codes by a collection of tenant ids returns the seeded relation. */
    @Test
    fun groupingSystemCodesByTenantIds() {
        val grouping = sysTenantSystemService.groupingSystemCodesByTenantIds(listOf(tenantId))
        assertTrue(grouping[tenantId]?.contains(subSystemCode) == true)
        // null argument -> query all
        assertTrue(sysTenantSystemService.groupingSystemCodesByTenantIds(null).isNotEmpty())
    }

    /** Grouping tenant ids by a collection of system codes returns the seeded relation. */
    @Test
    fun groupingTenantIdsBySystemCodes() {
        val grouping = sysTenantSystemService.groupingTenantIdsBySystemCodes(listOf(subSystemCode))
        assertTrue(grouping[subSystemCode]?.contains(tenantId) == true)
        // null argument -> query all
        assertTrue(sysTenantSystemService.groupingTenantIdsBySystemCodes(null).isNotEmpty())
    }

    /** batchBind with an empty collection short-circuits and inserts nothing. */
    @Test
    fun batchBind_emptyReturnsZero() {
        assertEquals(0, sysTenantSystemService.batchBind(tenantId, emptyList()))
    }

    /** batchBind where every requested relation already exists inserts nothing. */
    @Test
    fun batchBind_allExistReturnsZero() {
        // seeded relation already binds tenantId -> subSystemCode
        assertEquals(0, sysTenantSystemService.batchBind(tenantId, listOf(subSystemCode)))
    }

    /** batchBind inserts only the diff (the codes not already bound). */
    @Test
    fun batchBind_insertsOnlyDiff() {
        val inserted = sysTenantSystemService.batchBind(tenantId, listOf(subSystemCode, rootSystemCode))
        assertEquals(1, inserted)
        assertTrue(sysTenantSystemService.exists(tenantId, rootSystemCode))
    }

    /** unbind a non-existent relation returns false (warn branch). */
    @Test
    fun unbind_missingRelationReturnsFalse() {
        assertFalse(sysTenantSystemService.unbind(tenantId, "no-such-system-code"))
    }

    /** deleteByTenantId removes all relations for the tenant and returns the count. */
    @Test
    fun deleteByTenantId() {
        val count = sysTenantSystemService.deleteByTenantId(tenantId)
        assertTrue(count >= 1)
        assertTrue(sysTenantSystemService.searchSystemCodesByTenantId(tenantId).isEmpty())
        // deleting again removes nothing
        assertEquals(0, sysTenantSystemService.deleteByTenantId(tenantId))
    }

    /** batchDeleteByTenantIds with empty input short-circuits to 0; with data removes relations. */
    @Test
    fun batchDeleteByTenantIds() {
        assertEquals(0, sysTenantSystemService.batchDeleteByTenantIds(emptyList()))
        val count = sysTenantSystemService.batchDeleteByTenantIds(listOf(tenantId))
        assertTrue(count >= 1)
        assertTrue(sysTenantSystemService.searchSystemCodesByTenantId(tenantId).isEmpty())
    }

    /** insert creates a new relation and publishes the bound event; the row is retrievable. */
    @Test
    fun insert_createsRelation() {
        val id = sysTenantSystemService.insert(SysTenantSystem {
            this.tenantId = this@SysTenantSystemServiceTest.tenantId
            this.systemCode = rootSystemCode
        })
        assertTrue(id.isNotBlank())
        assertTrue(sysTenantSystemService.exists(tenantId, rootSystemCode))
    }

    /** deleteById removes an existing relation; deleting a missing id returns false (warn branch). */
    @Test
    fun deleteById_successAndMissing() {
        val id = sysTenantSystemService.insert(SysTenantSystem {
            this.tenantId = this@SysTenantSystemServiceTest.tenantId
            this.systemCode = rootSystemCode
        })
        assertTrue(sysTenantSystemService.deleteById(id))
        assertFalse(sysTenantSystemService.deleteById(id), "second delete of same id returns false")
        assertFalse(sysTenantSystemService.deleteById("00000000-0000-0000-0000-000000000099"))
    }

    /** batchDelete removes multiple relations and reports the actual count. */
    @Test
    fun batchDelete() {
        val id1 = sysTenantSystemService.insert(SysTenantSystem {
            this.tenantId = this@SysTenantSystemServiceTest.tenantId
            this.systemCode = rootSystemCode
        })
        val id2 = sysTenantSystemService.insert(SysTenantSystem {
            this.tenantId = this@SysTenantSystemServiceTest.tenantId
            this.systemCode = "svc-system-ts-test-extra"
        })
        val count = sysTenantSystemService.batchDelete(listOf(id1, id2))
        assertEquals(2, count)
        assertFalse(sysTenantSystemService.exists(tenantId, rootSystemCode))
        assertFalse(sysTenantSystemService.exists(tenantId, "svc-system-ts-test-extra"))
    }

    /** batchDelete of non-existent ids deletes nothing (count 0, no event). */
    @Test
    fun batchDelete_noMatchReturnsZero() {
        assertEquals(0, sysTenantSystemService.batchDelete(listOf("00000000-0000-0000-0000-0000000000aa")))
    }
}
