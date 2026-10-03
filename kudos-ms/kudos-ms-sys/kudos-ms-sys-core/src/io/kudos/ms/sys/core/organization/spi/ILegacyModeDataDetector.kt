package io.kudos.ms.sys.core.organization.spi

/**
 * Counts legacy (non-organization) data that would make turning organization mode on unsafe.
 *
 * Organization mode supports greenfield deployments only. Each module that owns tenant-scoped
 * identity data contributes a detector; the startup guard sums them before writing the enable marker.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface ILegacyModeDataDetector {

    /**
     * @param platformTenantIds tenants that keep the legacy model in organization mode
     * @return label → count of legacy rows found, e.g. `"tenants" to 5`; empty or zero when clean
     */
    fun countLegacyData(platformTenantIds: Set<String>): Map<String, Int>
}
