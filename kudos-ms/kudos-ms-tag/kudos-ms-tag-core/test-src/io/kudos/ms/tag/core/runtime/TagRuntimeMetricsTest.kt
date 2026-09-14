package io.kudos.ms.tag.core.runtime

import io.kudos.ms.tag.core.runtime.job.TagJobHealthSnapshot
import io.kudos.ms.tag.core.runtime.job.TagRuntimeHealthIndicator
import io.kudos.ms.tag.core.runtime.metrics.TagRuntimeMetrics
import io.kudos.ms.tag.core.platform.init.TagAutoConfiguration
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.boot.health.contributor.Status
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class TagRuntimeMetricsTest {

    @Test
    fun `runtime exposes exact low cardinality meter contract`() {
        val registry = SimpleMeterRegistry()
        val metrics = TagRuntimeMetrics(registry)

        metrics.factReceived()
        metrics.factDuplicate()
        metrics.factRejected()
        metrics.pendingJobs(7)
        metrics.jobProcessed(Duration.ofMillis(250))
        metrics.jobRetried()
        metrics.jobFailed()
        metrics.ruleEvaluated(Duration.ofMillis(10))
        metrics.ruleMatched()
        metrics.assignmentChanged()
        metrics.materializationLag(Duration.ofSeconds(3))
        metrics.queryCompleted(Duration.ofMillis(4), 12)

        assertEquals(EXPECTED_METERS, registry.meters.mapTo(sortedSetOf()) { it.id.name })
        registry.meters.forEach { meter ->
            val tagKeys = meter.id.tags.map { it.key }.toSet()
            assertFalse(tagKeys.any { it in FORBIDDEN_TAG_KEYS }, "${meter.id.name} has a high-cardinality tag")
        }
        assertEquals(1.0, registry.get("tag_fact_received_total").counter().count())
        assertEquals(7.0, registry.get("tag_job_pending").gauge().value())
        assertEquals(12.0, registry.get("tag_query_result_size").summary().max())
    }

    @Test
    fun `health reports database workers backlog failures rules and flyway`() {
        val indicator = TagRuntimeHealthIndicator(
            rdbProbe = { true },
            jobProbe = { TagJobHealthSnapshot(2, Duration.ofSeconds(45), 3) },
            publishedRuleProbe = { true },
            flywayVersionProbe = { "1.0.0" },
        )

        val health = indicator.health()

        assertEquals(Status.UP, health.status)
        assertEquals(true, health.details["rdb"])
        assertEquals(2, health.details["workerLeaseActivity"])
        assertEquals(45L, health.details["oldestPendingJobSeconds"])
        assertEquals(3L, health.details["failedJobCount"])
        assertEquals(true, health.details["publishedRuleCompilability"])
        assertEquals("1.0.0", health.details["flywayVersion"])
    }

    @Test
    fun `micrometer adapter is created only when the host supplies a registry`() {
        val runner = ApplicationContextRunner()
            .withUserConfiguration(TagAutoConfiguration.MicrometerConfiguration::class.java)
        runner.run { context -> assertEquals(0, context.getBeanNamesForType(TagRuntimeMetrics::class.java).size) }
        runner
            .withBean(MeterRegistry::class.java, { SimpleMeterRegistry() })
            .run { context -> assertEquals(1, context.getBeanNamesForType(TagRuntimeMetrics::class.java).size) }
    }

    private companion object {
        val EXPECTED_METERS = sortedSetOf(
            "tag_fact_received_total",
            "tag_fact_duplicate_total",
            "tag_fact_rejected_total",
            "tag_job_pending",
            "tag_job_processing_seconds",
            "tag_job_retry_total",
            "tag_job_failed_total",
            "tag_rule_evaluation_seconds",
            "tag_rule_match_total",
            "tag_assignment_change_total",
            "tag_materialization_lag_seconds",
            "tag_query_seconds",
            "tag_query_result_size",
        )
        val FORBIDDEN_TAG_KEYS = setOf("tenant", "tenant_id", "subject", "subject_id", "tag", "tag_code", "rule", "rule_id")
    }
}
