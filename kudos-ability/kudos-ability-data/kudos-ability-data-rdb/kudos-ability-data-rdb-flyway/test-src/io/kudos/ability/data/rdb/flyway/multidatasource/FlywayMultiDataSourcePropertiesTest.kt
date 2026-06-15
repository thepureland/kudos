package io.kudos.ability.data.rdb.flyway.multidatasource

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure unit tests for [FlywayMultiDataSourceProperties] (no Spring container). Covers:
 * - parseModules normalization of all accepted value shapes: CSV string (with blanks/extra commas),
 *   YAML list (Iterable, including null/blank elements), indexed-map flattening (relaxed-binder
 *   shape), null, and non-string scalars
 * - reserved-key defense in getDatasourceModules (mis-indented `execution-order` filtered out)
 * - getDataSourceKey reverse lookup hit/miss
 * - isReservedDatasourceConfigKey edge cases: null / empty / blank / unrelated keys
 * - AutoConfig default + Unicode module names round-trip
 *
 * @author K
 * @since 1.0.0
 */
internal class FlywayMultiDataSourcePropertiesTest {

    @Test
    fun csvValueIsTrimmedAndBlanksDropped() {
        val props = FlywayMultiDataSourceProperties()
        props.datasourceConfig = linkedMapOf("ds1" to " a , ,b ,, c ")
        assertEquals(listOf("a", "b", "c"), props.getDatasourceModules()["ds1"])
    }

    @Test
    fun iterableValueKeepsOrderAndDropsNullAndBlank() {
        val props = FlywayMultiDataSourceProperties()
        props.datasourceConfig = linkedMapOf("ds1" to listOf("m1", null, " ", " m2 "))
        assertEquals(listOf("m1", "m2"), props.getDatasourceModules()["ds1"])
    }

    /** Spring's relaxed binder can flatten a YAML list under a Map value into {0=x, 1=y}. */
    @Test
    fun indexedMapValueIsFlattenedInInsertionOrder() {
        val props = FlywayMultiDataSourceProperties()
        props.datasourceConfig = linkedMapOf("ds1" to linkedMapOf(0 to "x", 1 to " ", 2 to null, 3 to "y"))
        assertEquals(listOf("x", "y"), props.getDatasourceModules()["ds1"])
    }

    @Test
    fun nullValueYieldsEmptyList() {
        val props = FlywayMultiDataSourceProperties()
        props.datasourceConfig = linkedMapOf("ds1" to null)
        assertEquals(emptyList(), props.getDatasourceModules()["ds1"])
    }

    /** Non-string scalar values go through toString + CSV split. */
    @Test
    fun nonStringScalarValueIsStringified() {
        val props = FlywayMultiDataSourceProperties()
        props.datasourceConfig = linkedMapOf("ds1" to 42)
        assertEquals(listOf("42"), props.getDatasourceModules()["ds1"])
    }

    @Test
    fun unicodeModuleNamesAreKeptVerbatim() {
        val props = FlywayMultiDataSourceProperties()
        props.datasourceConfig = linkedMapOf("ds1" to "模块一, módulo-2")
        assertEquals(listOf("模块一", "módulo-2"), props.getDatasourceModules()["ds1"])
        assertEquals("ds1", props.getDataSourceKey("模块一"))
    }

    @Test
    fun reservedKeysAreFilteredFromDatasourceModules() {
        val props = FlywayMultiDataSourceProperties()
        props.datasourceConfig = linkedMapOf(
            "execution-order" to "ds1,ds2",
            "execution-order[0]" to "ds1",
            "real_ds" to "m1"
        )
        val modules = props.getDatasourceModules()
        assertEquals(setOf("real_ds"), modules.keys)
        assertEquals(listOf("m1"), modules["real_ds"])
    }

    @Test
    fun getDataSourceKeyReturnsNullWhenAbsentAndDeclarationOrderWins() {
        val props = FlywayMultiDataSourceProperties()
        props.datasourceConfig = linkedMapOf("ds1" to "shared", "ds2" to "shared,other")
        // first declaring data source wins for a module hosted by multiple entries
        assertEquals("ds1", props.getDataSourceKey("shared"))
        assertEquals("ds2", props.getDataSourceKey("other"))
        assertNull(props.getDataSourceKey("missing"))
        assertNull(props.getDataSourceKey(""))
    }

    @Test
    fun isReservedDatasourceConfigKeyEdgeCases() {
        assertTrue(FlywayMultiDataSourceProperties.isReservedDatasourceConfigKey("execution-order"))
        assertTrue(FlywayMultiDataSourceProperties.isReservedDatasourceConfigKey("execution-order[5]"))
        assertFalse(FlywayMultiDataSourceProperties.isReservedDatasourceConfigKey(null))
        assertFalse(FlywayMultiDataSourceProperties.isReservedDatasourceConfigKey(""))
        assertFalse(FlywayMultiDataSourceProperties.isReservedDatasourceConfigKey("   "))
        assertFalse(FlywayMultiDataSourceProperties.isReservedDatasourceConfigKey("execution-orderX"))
        assertFalse(FlywayMultiDataSourceProperties.isReservedDatasourceConfigKey("ds1"))
    }

    @Test
    fun defaultsAreEmptyAndAutoConfigDisabled() {
        val props = FlywayMultiDataSourceProperties()
        assertTrue(props.getDatasourceModules().isEmpty())
        assertTrue(props.executionOrder.isEmpty())
        assertFalse(props.autoConfig.enabled)
    }
}
