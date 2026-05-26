package io.kudos.test.rdb

import io.kudos.test.container.containers.H2TestContainer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * Base class for all test cases that require a relational database environment.
 *
 * ## Background and Use Cases
 * - Located in the test layer, extends [SqlTestBase] to provide container configuration for tests needing a full database
 * - Used by test classes that require a relational database
 * - Inherits the script file configuration capability from [SqlTestBase]
 *
 * ## Responsibilities
 * - Starts and configures H2TestContainer
 * - Inherits all functionality of [SqlTestBase] (test data loading, serial execution, transaction rollback)
 *
 * ## Core Flow
 * 1. Start H2TestContainer inside @DynamicPropertySource
 * 2. Inherit test data loading and transaction rollback functionality from [SqlTestBase]
 *
 * ## Dependencies and External Interactions
 * - Depends on: [SqlTestBase] (provides test data loading, transaction management)
 * - Depends on: H2TestContainer
 * - IO: starts Docker containers (if not running)
 *
 * ## Contract
 * - Input: none
 * - Output: configures Spring test environment properties (H2)
 * - Errors: throws exceptions if Docker is not installed or container startup fails
 *
 * ## Transactions and Consistency
 * - Transactions: inherits @Transactional from [SqlTestBase]; fixture data is committed outside the transaction by @BeforeTransaction (see [SqlTestBase] docs)
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
open class RdbTestBase : SqlTestBase() {

    companion object Companion {
        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "false" }
            // Default H2; to switch to Postgres/MySQL, business subclasses should write their own companion + @DynamicPropertySource override
            H2TestContainer.startIfNeeded(registry)
        }
    }

}