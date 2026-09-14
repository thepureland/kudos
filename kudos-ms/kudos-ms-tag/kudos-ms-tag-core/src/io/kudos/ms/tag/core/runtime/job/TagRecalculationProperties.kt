package io.kudos.ms.tag.core.runtime.job

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "kudos.tag.recalculation")
data class TagRecalculationProperties(
    val schedulingEnabled: Boolean = false,
    val batchSize: Int = 20,
    val leaseDuration: Duration = Duration.ofMinutes(1),
    val pollDelay: Duration = Duration.ofSeconds(5),
    val maximumAttempts: Int = 10,
    val synchronousSubjectLimit: Int = 100,
    val synchronousDirectRuleLimit: Int = 200,
) {
    init {
        require(batchSize in 1..100) { "Recalculation batch size must be between 1 and 100." }
        require(!leaseDuration.isNegative && !leaseDuration.isZero) { "Recalculation lease duration must be positive." }
        require(!pollDelay.isNegative && !pollDelay.isZero) { "Recalculation poll delay must be positive." }
        require(maximumAttempts > 0) { "Recalculation maximum attempts must be positive." }
        require(synchronousSubjectLimit in 1..100) { "Synchronous subject limit must be between 1 and 100." }
        require(synchronousDirectRuleLimit in 1..200) { "Synchronous direct-rule limit must be between 1 and 200." }
    }
}
