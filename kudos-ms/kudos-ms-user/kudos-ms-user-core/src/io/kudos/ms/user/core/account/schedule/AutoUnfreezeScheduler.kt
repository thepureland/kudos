package io.kudos.ms.user.core.account.schedule

import io.kudos.base.logger.LogFactory
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component


/**
 * 自动清理过期冻结记录的定时任务。
 *
 * **重要**：本类的 `@Scheduled` 注解仅在消费方启用 `@EnableScheduling` 时才会被识别。
 * kudos-ms-user-core 自身**不**强制启用调度——保持库模式，由部署方按需开启：
 *
 * ```kotlin
 * @EnableKudos
 * @EnableScheduling     // ← 启用后下面的 @Scheduled 才会跑
 * class UserApiAdminApplication
 * ```
 *
 * cron 表达式默认每小时整点跑一次。如需覆盖：在 application.yml 配
 * `kudos.user.auto-unfreeze.cron: 0 0/15 * * * *`（每 15 分钟）。
 *
 * 不需要分布式锁（同 [io.kudos.context.retry.FailedDataRetryScanner] 那种）的原因：
 * - 操作幂等（DB SQL 跑两遍结果相同）
 * - 频次极低（小时级），并发触发的概率小
 * - 单次执行写入量小（通常 0-个位数）
 *
 * 如需在多实例下严格只跑一次，再叠加 `@SchedulerLock`（ShedLock）即可，不在本类内置。
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
                log.info("AutoUnfreezeScheduler 清理 ${cleared} 条过期冻结")
            }
        } catch (e: Exception) {
            // 调度任务里不要让异常抛出，避免被调度器记为"失败"并拖累后续触发
            log.error("AutoUnfreezeScheduler 执行失败", e)
        }
    }
}
