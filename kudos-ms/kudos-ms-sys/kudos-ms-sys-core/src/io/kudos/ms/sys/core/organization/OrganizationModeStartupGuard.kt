package io.kudos.ms.sys.core.organization

import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.core.organization.dao.SysOrganizationModeDao
import io.kudos.ms.sys.core.organization.spi.ILegacyModeDataDetector
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.stereotype.Component

/**
 * Enforces the one-way organization mode switch at startup (G-18, G-19).
 *
 * | switch | marker | result |
 * |---|---|---|
 * | off | absent | legacy mode |
 * | on | absent | refuse if any legacy data exists, otherwise write the marker |
 * | on | present | start; legacy data is no longer re-checked |
 * | off | present | refuse: organization mode cannot be turned off once used |
 *
 * Runs after all singletons exist, so database migrations have already been applied. After the
 * marker is written, "legacy data never passes" is guaranteed at run time: a non-platform tenant
 * without an organization, or an account without one, cannot satisfy organization authorization.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class OrganizationModeStartupGuard(
    private val mode: OrganizationMode,
    private val markerDao: SysOrganizationModeDao,
    private val detectors: List<ILegacyModeDataDetector>,
) : SmartInitializingSingleton {

    private val log = LogFactory.getLog(this::class)

    override fun afterSingletonsInstantiated() {
        val marked = markerDao.isMarked()
        if (!mode.enabled) {
            check(!marked) {
                "Organization mode was enabled on this database and cannot be turned off. " +
                    "Set kudos.ms.organization.enabled=true and fix forward instead."
            }
            return
        }
        if (marked) return
        val found = detectors
            .flatMap { it.countLegacyData(mode.platformTenantIds()).entries }
            .filter { it.value > 0 }
        check(found.isEmpty()) {
            "Organization mode supports new deployments only, but legacy data was found (" +
                found.joinToString { "${it.key}=${it.value}" } +
                "). Migrate it with an external tool first, or keep kudos.ms.organization.enabled=false."
        }
        markerDao.mark()
        log.info("Organization mode enabled for the first time; marker written.")
    }
}
