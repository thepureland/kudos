package io.kudos.ms.tag.core.runtime.job

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import java.time.Duration
import java.time.Instant

data class TagJobHealthSnapshot(
    val activeWorkerLeases: Int,
    val oldestPendingJobAge: Duration?,
    val failedJobCount: Long,
)

/** Actuator adapter built from small probes so operational checks stay testable and replaceable. */
open class TagRuntimeHealthIndicator(
    private val rdbProbe: () -> Boolean,
    private val jobProbe: (Instant) -> TagJobHealthSnapshot,
    private val publishedRuleProbe: () -> Boolean,
    private val flywayVersionProbe: () -> String?,
    private val now: () -> Instant = Instant::now,
) : HealthIndicator {
    override fun health(): Health {
        val rdbResult = runCatching(rdbProbe)
        val jobsResult = runCatching { jobProbe(now()) }
        val rulesResult = runCatching(publishedRuleProbe)
        val flywayResult = runCatching(flywayVersionProbe)
        val rdb = rdbResult.getOrDefault(false)
        val jobs = jobsResult.getOrDefault(TagJobHealthSnapshot(0, null, 0))
        val rulesCompilable = rulesResult.getOrDefault(false)
        val builder = if (rdb && rulesCompilable && jobsResult.isSuccess) Health.up() else Health.down()
        listOf(rdbResult, jobsResult, rulesResult, flywayResult).firstOrNull { it.isFailure }
            ?.exceptionOrNull()
            ?.let(builder::withException)
        return builder
            .withDetail("rdb", rdb)
            .withDetail("workerLeaseActivity", jobs.activeWorkerLeases)
            .withDetail("oldestPendingJobSeconds", jobs.oldestPendingJobAge?.seconds ?: 0L)
            .withDetail("failedJobCount", jobs.failedJobCount)
            .withDetail("publishedRuleCompilability", rulesCompilable)
            .withDetail("flywayVersion", flywayResult.getOrNull() ?: "unknown")
            .build()
    }
}
