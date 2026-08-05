package io.kudos.ms.auth.core.role.temporal.schedule

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupUserService
import io.kudos.ms.auth.core.role.temporal.service.iservice.IAuthRoleUserTemporalService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime


/**
 * Scheduled sweep over both edges of every temporal grant's validity window.
 *
 * - **Expiry**: deletes every `auth_role_user` row whose `end_time` has passed and evicts the
 *   affected users' permission caches, so expired grants stop granting access without waiting for
 *   a manual purge.
 * - **Activation**: evicts the caches of users whose `start_time` just arrived. Without this the
 *   cached snapshot — computed when the future-dated grant was written, and correctly excluding it
 *   at that moment — would never be recomputed, and the grant would silently never take effect.
 * - **Group memberships**: the same two edges on `auth_group_user`. A windowed membership hands
 *   over every role the group carries, so it needs the identical treatment — otherwise "add them to
 *   the group instead" quietly becomes the way around an expiry somebody set. Memberships are
 *   announced rather than deleted; see [IAuthGroupUserService.announceWindowChanges].
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
 * the rows; the second finds nothing and exits immediately. Re-running the activation sweep only
 * re-evicts caches, which is likewise harmless.
 *
 * Distributed deployments: the operation is idempotent and involves very few rows per run, so
 * concurrent sweeps from multiple instances are harmless. For exactly-once semantics, layer
 * `@SchedulerLock` (ShedLock) on top of [run] — not bundled here.
 *
 * Restart behaviour: [lastSweepAt] starts at bean-construction time, so grants that opened while
 * the process was down are not re-announced by this scheduler. They do not need to be — a cold
 * start reloads the permission caches from the database, which evaluates every window afresh.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class ExpiredGrantPurgeScheduler(
    private val temporalService: IAuthRoleUserTemporalService,
    private val groupUserService: IAuthGroupUserService,
) {

    private val log = LogFactory.getLog(this::class)

    /**
     * Lower bound of the next activation sweep. Only ever touched from [run]; the scheduler runs a
     * job single-threadedly, and a concurrent run from another instance merely repeats an
     * idempotent eviction.
     */
    @Volatile
    private var lastSweepAt: LocalDateTime = LocalDateTime.now()

    @Scheduled(cron = "\${kudos.auth.expired-grant-purge.cron:0 * * * * *}")
    open fun run() {
        try {
            val purged = temporalService.purgeExpired()
            if (purged > 0) {
                log.info("ExpiredGrantPurgeScheduler purged ${purged} expired role-user grant(s)")
            }

            // Capture `now` before the sweep so a grant starting mid-run lands in the next window
            // rather than being skipped by both.
            val now = LocalDateTime.now()
            val since = lastSweepAt
            val activated = temporalService.activateStarted(since, now)
            // Both edges in one call: unlike role grants, a membership that lapses is not deleted,
            // so its expiry has no write event either and needs announcing just as its start does.
            val memberships = groupUserService.announceWindowChanges(since, now)
            lastSweepAt = now
            if (activated > 0) {
                log.info("ExpiredGrantPurgeScheduler activated ${activated} role-user grant(s) whose window opened")
            }
            if (memberships > 0) {
                log.info("ExpiredGrantPurgeScheduler announced ${memberships} group membership window change(s)")
            }
        } catch (e: Exception) {
            // Never let exceptions escape from a scheduled job; otherwise the scheduler marks the
            // job as failed and may suppress future triggers.
            log.error("ExpiredGrantPurgeScheduler execution failed", e)
        }
    }
}
