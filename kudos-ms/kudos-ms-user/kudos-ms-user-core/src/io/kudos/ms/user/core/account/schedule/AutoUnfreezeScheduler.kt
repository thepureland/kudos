package io.kudos.ms.user.core.account.schedule

import io.kudos.base.logger.LogFactory
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


/**
 * Scheduled job that automatically cleans expired freeze records.
 *
 * **Important**: the `@Scheduled` annotation on this class only takes effect when the consumer enables
 * `@EnableScheduling`. kudos-ms-user-core itself does **not** force-enable scheduling -- it stays in
 * library mode and the deployer turns it on as needed:
 *
 * ```kotlin
 * @EnableKudos
 * @EnableScheduling     // <- the @Scheduled below only runs after this is enabled
 * class UserApiAdminApplication
 * ```
 *
 * The cron expression defaults to running once at the top of every hour. To override, set
 * `kudos.user.auto-unfreeze.cron: 0 0/15 * * * *` in application.yml (every 15 minutes).
 *
 * No distributed lock is needed (unlike [io.kudos.context.retry.FailedDataRetryScanner]) because:
 * - the operation is idempotent (running the DB SQL twice yields the same result)
 * - the frequency is very low (hourly), so concurrent triggers are unlikely
 * - each run writes a small number of rows (typically 0 to single digits)
 *
 * If exactly-once execution across instances is required, layer `@SchedulerLock` (ShedLock) on top --
 * not built in here.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class AutoUnfreezeScheduler(
    private val userAccountService: IUserAccountService,
) {

    private val log = LogFactory.getLog(this::class)

    @Scheduled(cron = "\${kudos.user.auto-unfreeze.cron:0 0 * * * *}")
    open fun run() {
        try {
            val cleared = userAccountService.cleanExpiredFreezes()
            if (cleared > 0) {
                log.info("AutoUnfreezeScheduler cleaned ${cleared} expired freeze records")
            }
        } catch (e: Exception) {
            // Never let exceptions escape from the scheduled job; otherwise the scheduler marks it as
            // "failed" and may drag down subsequent triggers.
            log.error("AutoUnfreezeScheduler execution failed", e)
        }
    }
}
