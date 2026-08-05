package io.kudos.ms.sys.core.accessrule.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure unit test for access-rule and IP-access-rule domain event data classes.
 * Focuses on the derived properties that contain logic: [SysAccessRuleUpdated.dimensionChanged] and the
 * contract-satisfying getters of the batch events ([SysAccessRuleBatchDeleted.id],
 * [SysAccessRuleIpBatchDeleted.id] / [parentSystemCode] / [parentTenantId]).
 *
 * No Spring context or database required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AccessRuleEventsTest {

    // region SysAccessRuleUpdated.dimensionChanged ------------------------------------------------

    @Test
    fun dimensionChanged_false_whenBeforeSystemCodeNull() {
        val e = SysAccessRuleUpdated(
            id = "1", systemCode = "s", tenantId = "t",
            beforeSystemCode = null, beforeTenantId = "t",
        )
        assertFalse(e.dimensionChanged)
    }

    @Test
    fun dimensionChanged_false_whenSameDimension() {
        val e = SysAccessRuleUpdated(
            id = "1", systemCode = "s", tenantId = "t",
            beforeSystemCode = "s", beforeTenantId = "t",
        )
        assertFalse(e.dimensionChanged)
    }

    @Test
    fun dimensionChanged_true_whenSystemCodeChanged() {
        val e = SysAccessRuleUpdated(
            id = "1", systemCode = "s2", tenantId = "t",
            beforeSystemCode = "s1", beforeTenantId = "t",
        )
        assertTrue(e.dimensionChanged)
    }

    @Test
    fun dimensionChanged_true_whenTenantIdChanged() {
        val e = SysAccessRuleUpdated(
            id = "1", systemCode = "s", tenantId = "t2",
            beforeSystemCode = "s", beforeTenantId = "t1",
        )
        assertTrue(e.dimensionChanged)
    }

    @Test
    fun dimensionChanged_true_whenTenantIdChangedToNull() {
        val e = SysAccessRuleUpdated(
            id = "1", systemCode = "s", tenantId = null,
            beforeSystemCode = "s", beforeTenantId = "t1",
        )
        assertTrue(e.dimensionChanged)
    }

    // region simple event identity / fields -------------------------------------------------------

    @Test
    fun inserted_fields() {
        val e = SysAccessRuleInserted(id = "id1", systemCode = "s", tenantId = null)
        assertEquals("id1", e.id)
        assertEquals("s", e.systemCode)
        assertEquals(null, e.tenantId)
    }

    @Test
    fun deleted_fields() {
        val e = SysAccessRuleDeleted(id = "id2", systemCode = "s", tenantId = "t")
        assertEquals("id2", e.id)
        assertEquals("t", e.tenantId)
    }

    @Test
    fun batchDeleted_idReturnsFirst() {
        val e = SysAccessRuleBatchDeleted(
            ids = listOf("a", "b", "c"),
            dimensions = listOf("s" to "t"),
        )
        assertEquals("a", e.id)
        assertEquals(listOf("s" to "t"), e.dimensions)
    }

    // region IP events ----------------------------------------------------------------------------

    @Test
    fun ipInserted_fields() {
        val e = SysAccessRuleIpInserted(
            id = "ip1", parentSystemCode = "s", parentTenantId = "t", active = true,
        )
        assertEquals("ip1", e.id)
        assertEquals("s", e.parentSystemCode)
        assertEquals("t", e.parentTenantId)
        assertTrue(e.active)
    }

    @Test
    fun ipUpdated_fields() {
        val e = SysAccessRuleIpUpdated(
            id = "ip2", parentSystemCode = "s", parentTenantId = null, active = false,
        )
        assertEquals("ip2", e.id)
        assertEquals(null, e.parentTenantId)
        assertFalse(e.active)
    }

    @Test
    fun ipDeleted_fields() {
        val e = SysAccessRuleIpDeleted(id = "ip3", parentSystemCode = "s", parentTenantId = "t")
        assertEquals("ip3", e.id)
        assertEquals("s", e.parentSystemCode)
    }

    @Test
    fun ipBatchDeleted_derivedGetters_useFirstDimension() {
        val e = SysAccessRuleIpBatchDeleted(
            ids = listOf("x", "y"),
            dimensions = listOf("sysA" to "tenA", "sysB" to null),
        )
        assertEquals("x", e.id)
        assertEquals("sysA", e.parentSystemCode)
        assertEquals("tenA", e.parentTenantId)
    }

    @Test
    fun ipBatchDeleted_derivedGetters_firstDimensionNullTenant() {
        val e = SysAccessRuleIpBatchDeleted(
            ids = listOf("only"),
            dimensions = listOf("sysC" to null),
        )
        assertEquals("only", e.id)
        assertEquals("sysC", e.parentSystemCode)
        assertEquals(null, e.parentTenantId)
    }
}
