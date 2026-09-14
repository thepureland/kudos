package io.kudos.ms.tag.core.runtime.job

import java.time.Duration
import kotlin.math.pow

class RecalculationRetryPolicy(
    private val initialDelay: Duration = Duration.ofSeconds(5),
    private val multiplier: Double = 2.0,
    private val maximumDelay: Duration = Duration.ofMinutes(5),
) {
    init {
        require(!initialDelay.isNegative && !initialDelay.isZero) { "Initial retry delay must be positive." }
        require(multiplier >= 1.0) { "Retry multiplier must be at least one." }
        require(maximumDelay >= initialDelay) { "Maximum retry delay must not be smaller than the initial delay." }
    }

    fun delayForAttempt(attempt: Int): Duration {
        require(attempt > 0) { "Attempt number must be positive." }
        val factor = multiplier.pow((attempt - 1).toDouble())
        val millis = (initialDelay.toMillis() * factor).coerceAtMost(maximumDelay.toMillis().toDouble()).toLong()
        return Duration.ofMillis(millis)
    }
}
