package io.kudos.test.rdb

import io.kudos.test.container.containers.H2TestContainer
import io.kudos.test.container.containers.RedisTestContainer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.concurrent.thread

/**
 * Base class for all test cases that require both a relational database and Redis cache environment.
 *
 * ## Background and Use Cases
 * - Located in the test layer, extends [SqlTestBase] to provide container configuration for tests needing a full database and Redis environment
 * - Used by Service test classes and other tests that require a complete environment
 * - Note: this class does NOT extend [RdbTestBase] — the two have mutually exclusive configuration for `kudos.ability.cache.enabled` and cannot be combined
 *
 * ## Responsibilities
 * - Starts and configures H2TestContainer and RedisTestContainer
 * - Configures cache strategy
 * - Inherits all functionality of [SqlTestBase] (test data loading, serial execution, transaction rollback)
 * - Resets Redis + application caches after fixture loading via [CacheTestResetSupport]
 *
 * ## Core Flow
 * 1. Start H2TestContainer and RedisTestContainer in parallel inside @DynamicPropertySource
 * 2. Configure cache-related properties (enable cache, default SINGLE_LOCAL strategy)
 * 3. Inherit test data loading and transaction rollback functionality from [SqlTestBase]
 * 4. Flush Redis + reload application caches after each fixture load completes
 *
 * ## Dependencies and External Interactions
 * - Depends on: [SqlTestBase] (provides test data loading, transaction management)
 * - Depends on: H2TestContainer, RedisTestContainer
 * - IO: starts Docker containers (if not running)
 *
 * ## Contract
 * - Input: none
 * - Output: configures Spring test environment properties (H2, Redis, and cache related)
 * - Errors: throws exceptions if Docker is not installed or container startup fails
 *
 * ## Transactions and Consistency
 * - Transactions: inherits @Transactional from [SqlTestBase]; fixture data is committed outside the transaction by @BeforeTransaction
 *
 * ## Concurrency and Thread Safety
 * - Container startup uses synchronization to ensure each container is started only once
 * - Inherits serial execution guarantees from [SqlTestBase]
 *
 * ## Performance Characteristics
 * - Container startup has some overhead but already running containers are reused
 *
 * ## Security and Compliance
 * - For test environments only, does not involve production data
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
open class RdbAndRedisCacheTestBase : SqlTestBase() {

    override fun afterTestDataSetup() {
        CacheTestResetSupport.resetRedisAndApplicationCaches()
    }

    companion object Companion {

        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            val ryukDisabledKey = "testcontainers.ryuk.disabled"
            if (System.getProperty(ryukDisabledKey).isNullOrBlank()) {
                System.setProperty(ryukDisabledKey, "true")
            }
            registry.add("kudos.ability.cache.enabled") { "true" }
            // Default SINGLE_LOCAL; business subclasses should override when testing LOCAL_AND_REMOTE / SINGLE_REMOTE
            registry.add("cache.config.strategy") { "SINGLE_LOCAL" }

            val h2Thread = thread(name = "h2-testcontainer-start") { H2TestContainer.startIfNeeded(registry) }
            val redisThread = thread(name = "redis-testcontainer-start") { RedisTestContainer.startIfNeeded(registry) }

            h2Thread.join()
            redisThread.join()
        }
    }

}
