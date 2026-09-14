package io.kudos.ms.tag.core.runtime.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

/** Fixed, low-cardinality metric surface. Business identifiers belong in logs, never meter labels. */
open class TagRuntimeMetrics(registry: MeterRegistry) {
    private val factReceived = Counter.builder("tag_fact_received_total").register(registry)
    private val factDuplicate = Counter.builder("tag_fact_duplicate_total").register(registry)
    private val factRejected = Counter.builder("tag_fact_rejected_total").register(registry)
    private val pendingJobs = AtomicLong()
    private val jobProcessing = Timer.builder("tag_job_processing_seconds").register(registry)
    private val jobRetry = Counter.builder("tag_job_retry_total").register(registry)
    private val jobFailed = Counter.builder("tag_job_failed_total").register(registry)
    private val ruleEvaluation = Timer.builder("tag_rule_evaluation_seconds").register(registry)
    private val ruleMatch = Counter.builder("tag_rule_match_total").register(registry)
    private val assignmentChange = Counter.builder("tag_assignment_change_total").register(registry)
    private val materializationLagSeconds = AtomicLong()
    private val query = Timer.builder("tag_query_seconds").register(registry)
    private val queryResultSize = DistributionSummary.builder("tag_query_result_size").register(registry)

    init {
        Gauge.builder("tag_job_pending", pendingJobs) { it.get().toDouble() }.register(registry)
        Gauge.builder("tag_materialization_lag_seconds", materializationLagSeconds) { it.get().toDouble() }.register(registry)
    }

    open fun factReceived() = factReceived.increment()
    open fun factDuplicate() = factDuplicate.increment()
    open fun factRejected() = factRejected.increment()
    open fun pendingJobs(count: Long) = pendingJobs.set(count.coerceAtLeast(0))
    open fun jobProcessed(duration: Duration) = jobProcessing.record(duration)
    open fun jobRetried() = jobRetry.increment()
    open fun jobFailed() = jobFailed.increment()
    open fun ruleEvaluated(duration: Duration) = ruleEvaluation.record(duration)
    open fun ruleMatched() = ruleMatch.increment()
    open fun assignmentChanged() = assignmentChange.increment()
    open fun materializationLag(duration: Duration) = materializationLagSeconds.set(duration.seconds.coerceAtLeast(0))
    open fun queryCompleted(duration: Duration, resultSize: Int) {
        query.record(duration)
        queryResultSize.record(resultSize.coerceAtLeast(0).toDouble())
    }
}
