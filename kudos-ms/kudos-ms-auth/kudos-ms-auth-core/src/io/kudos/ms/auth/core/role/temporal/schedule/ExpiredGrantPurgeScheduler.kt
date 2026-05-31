package io.kudos.ms.auth.core.role.temporal.schedule

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.role.temporal.service.iservice.IAuthRoleUserTemporalService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


/**
 * Scheduled reaper for expired temporal role grants.
 *
 * Deletes every `auth_role_user` row whose `end_time` has passed and evicts the affected users'
 * permission caches, so expired grants stop granting access without waiting for a manual purge.
 *
 * **Activation**: this class is a passive `@Component` — the `@Scheduled` annotation only fires
 * when the deploying application enables scheduling:
 *
 * ```kotlin
 * @EnableKudos
 * @EnableScheduling     // required; kudos-ms-auth-core itself stays in library mode
 * class AuthApiAdminApplication
 * ```
 *
 * The default cron `0 * * * * *` runs once per minute, which is suitable for short-lived
 * grants. Override via `kudos.auth.expired-grant-purge.cron` in application.yml when a
 * less-frequent sweep is acceptable (e.g. `0 0 * * * *` = hourly).
 *
 * Idempotent: running the purge twice for the same instant is safe — the first call deletes
 * the rows; the second finds nothing and exits immediately.
 *
 * Distributed deployments: the operation is idempotent and involves very few rows per run, so
 * concurrent sweeps from multiple instances are harmless. For exactly-once semantics, layer
 * `@SchedulerLock` (ShedLock) on top of [run] — not bundled here.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class ExpiredGrantPurgeScheduler(
    private val temporalService: IAuthRoleUserTemporalService,
) {

    private val log = LogFactory.getLog(this::class)

    @Scheduled(cron = "\${kudos.auth.expired-grant-purge.cron:0 * * * * *}")
    open fun run() {
        try {
            val purged = temporalService.purgeExpired()
            if (purged > 0) {
                log.info("ExpiredGrantPurgeScheduler purged ${purged} expired role-user grant(s)")
            }
        } catch (e: Exception) {
            // Never let exceptions escape from a scheduled job; otherwise the scheduler marks the
            // job as failed and may suppress future triggers.
            log.error("ExpiredGrantPurgeScheduler execution failed", e)
        }
    }
}
