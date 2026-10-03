package io.kudos.ms.sys.core.organization

import io.kudos.ms.sys.core.organization.dao.SysOrganizationModeDao
import io.kudos.ms.sys.core.organization.init.OrganizationModeProperties
import io.kudos.ms.sys.core.organization.spi.ILegacyModeDataDetector
import org.springframework.mock.env.MockEnvironment
import kotlin.test.*

/**
 * The one-way switch (G-18, G-19) as a truth table over switch × marker × legacy data.
 */
internal class OrganizationModeStartupGuardTest {

    private class Marker(var marked: Boolean) : SysOrganizationModeDao() {
        override fun isMarked() = marked
        override fun mark() { marked = true }
    }

    private fun guard(enabled: Boolean, marker: Marker, legacy: Map<String, Int> = emptyMap()): OrganizationModeStartupGuard {
        val properties = OrganizationModeProperties().apply { this.enabled = enabled }
        val environment = MockEnvironment().withProperty(OrganizationMode.PLATFORM_TENANT_IDS_KEY, "platform")
        val detector = object : ILegacyModeDataDetector {
            override fun countLegacyData(platformTenantIds: Set<String>): Map<String, Int> {
                assertEquals(setOf("platform"), platformTenantIds)
                return legacy
            }
        }
        return OrganizationModeStartupGuard(OrganizationMode(properties, environment), marker, listOf(detector))
    }

    @Test
    fun offWithoutMarkerIsLegacyMode() {
        val marker = Marker(false)
        guard(enabled = false, marker = marker, legacy = mapOf("tenants" to 5)).afterSingletonsInstantiated()
        assertFalse(marker.marked)
    }

    @Test
    fun firstStartOnCleanDatabaseWritesMarker() {
        val marker = Marker(false)
        guard(enabled = true, marker = marker, legacy = mapOf("tenants" to 0)).afterSingletonsInstantiated()
        assertTrue(marker.marked)
    }

    @Test
    fun firstStartWithLegacyDataIsRefusedWithCounts() {
        val marker = Marker(false)
        val error = assertFailsWith<IllegalStateException> {
            guard(enabled = true, marker = marker, legacy = mapOf("tenants" to 5, "accounts" to 37)).afterSingletonsInstantiated()
        }
        assertContains(error.message!!, "tenants=5")
        assertContains(error.message!!, "accounts=37")
        assertFalse(marker.marked)
    }

    @Test
    fun markedDatabaseNoLongerRechecksLegacyData() {
        guard(enabled = true, marker = Marker(true), legacy = mapOf("tenants" to 3)).afterSingletonsInstantiated()
    }

    @Test
    fun turningOffAfterUseIsRefused() {
        assertFailsWith<IllegalStateException> { guard(enabled = false, marker = Marker(true)).afterSingletonsInstantiated() }
    }
}
