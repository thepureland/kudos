package io.kudos.ability.distributed.discovery.nacos.loadbalancer

import org.springframework.cloud.client.DefaultServiceInstance
import org.springframework.cloud.client.ServiceInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Pure-function unit tests for [HintZoneServiceInstanceListSupplier.filteredByHint].
 *
 * Main branches:
 *  - **hint non-empty, has match** -> returns only matching instances
 *  - **hint non-empty, no match** -> falls back to the full instance list (so business calls are not rejected outright)
 *  - **hint empty + default zone non-empty** -> returns only instances whose zone matches the default, or have no zone set
 *  - **hint empty + default zone empty** -> returns the list as-is
 *
 * Also verifies the configurable [HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY]
 * scenario: switching the metadata field name to `region` still hits.
 */
internal class HintZoneServiceInstanceListSupplierTest {

    private val east = instance("svc-east", "east")
    private val west = instance("svc-west", "west")
    private val none = instance("svc-none", null)

    @Test
    fun hintHit_returnsOnlyMatching() {
        val all = mutableListOf(east, west)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = "east", defaultZone = null,
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertEquals(listOf(east), result)
    }

    @Test
    fun hintMiss_returnsAllAsFallback() {
        val all = mutableListOf(east, west)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = "moon", defaultZone = null,
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        // No matches -> fall back to the original list (by reference, confirming the fast path)
        assertSame(all, result)
    }

    @Test
    fun noHint_defaultZoneSet_returnsMatchingPlusUntagged() {
        val all = mutableListOf(east, west, none)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = null, defaultZone = "east",
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertEquals(listOf(east, none), result, "Should include instances matching the default zone plus those with no zone set")
    }

    @Test
    fun noHint_blankDefaultZone_returnsAllAsIs() {
        val all = mutableListOf(east, west, none)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = null, defaultZone = "",
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertSame(all, result, "Should return as-is when there is no default zone")
    }

    @Test
    fun noHint_nullDefaultZone_returnsAllAsIs() {
        val all = mutableListOf(east, west, none)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = null, defaultZone = null,
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertSame(all, result)
    }

    @Test
    fun blankHint_treatedAsAbsent() {
        val all = mutableListOf(east, west)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = "   ", defaultZone = null,
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertSame(all, result, "Blank string should be treated the same as null")
    }

    @Test
    fun customMetadataKey_works() {
        val east = DefaultServiceInstance("svc-east", "svc", "h1", 80, false, mapOf("region" to "east"))
        val west = DefaultServiceInstance("svc-west", "svc", "h2", 80, false, mapOf("region" to "west"))
        val all = mutableListOf<ServiceInstance>(east, west)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = "east", defaultZone = null,
            zoneMetadataKey = "region",
        )
        assertEquals(1, result.size)
        assertTrue(result.contains(east))
    }

    private fun instance(id: String, zone: String?): ServiceInstance {
        val metadata = if (zone == null) emptyMap() else mapOf("zone" to zone)
        return DefaultServiceInstance(id, "svc", "host-$id", 80, false, metadata)
    }
}
