package io.kudos.test.rdb

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.base.io.scanner.classpath.ClassPathScanner
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.test.context.transaction.BeforeTransaction
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource

/**
 * Base class for all test cases that involve SQL operations.
 *
 * ## Background and Use Cases
 * - Located in the test layer, provides unified test data management and execution control for all SQL-based tests
 * - Directly extended by concrete DAO test classes
 * - Provides basic test data loading, serial execution, transaction rollback, and database container configuration
 *
 * ## Responsibilities
 * - Loads and executes the test data SQL file specified by the subclass
 * - Ensures tests run serially to prevent data contamination between tests
 * - Cleans up test data via transaction rollback
 *
 * ## Core Flow
 * 1. Subclass specifies the test data SQL file path via getTestDataSqlPath()
 * 2. In @BeforeTransaction, load and execute the specified SQL file (data is committed outside the transaction and is not rolled back)
 * 3. Writes done by the test method itself are rolled back by @Transactional; fixture data is retained and reloaded before the next test
 * 4. @Execution(ExecutionMode.SAME_THREAD) ensures tests run serially
 *
 * ## Dependencies and External Interactions
 * - Depends on: DataSource (obtained via DsContextProcessor), ResourceDatabasePopulator (provided by Spring)
 * - Depends on: Spring Test Context (enabled via @EnableKudosTest)
 * - IO: reads SQL files from the classpath and executes them
 *
 * ## Contract
 * - Input: subclass must implement getTestDataSqlPath() returning the SQL file path (relative to classpath)
 * - Output: no return value; prepares test data by executing the SQL file and configures Spring test environment properties
 * - Errors: throws exceptions if the SQL file does not exist or execution fails
 *
 * ## Transactions and Consistency
 * - Transactions: @Transactional rolls back writes from **the test method itself**; fixture data is committed outside the transaction in @BeforeTransaction (not rolled back)
 * - Consistency: the entire fixture SQL is re-executed before each test method to guarantee a consistent starting point
 *
 * ## Concurrency and Thread Safety
 * - @Execution(ExecutionMode.SAME_THREAD) ensures tests run serially
 * - Prevents data conflicts caused by multiple test methods running in parallel
 *
 * ## Performance Characteristics
 * - The SQL file is executed before every test method, incurring some overhead
 * - But this guarantees test data independence and repeatability
 *
 * ## Security and Compliance
 * - Only executes test data SQL files, no sensitive operations involved
 * - For test environments only, does not involve production data
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnableKudosTest
@Transactional
@Execution(ExecutionMode.SAME_THREAD)
open class SqlTestBase {

    /**
     * Gets the test data SQL file path.
     *
     * @return the SQL file path (relative to classpath); returns null when not found (logs a warning only, does not throw)
     */
    protected open fun getTestDataSqlPath(): String? {
        val rdbType = RdbKit.determineRdbTypeByDataSource(dataSource)
        val sqlFilename = getTestClassName()
        val parentPath = "sql/${rdbType.name.lowercase()}"
        val fullSqlFilename = "$parentPath/${sqlFilename}.sql"
        val files = ClassPathScanner.scanForResources(parentPath, sqlFilename, "sql")
        return if (files.isNotEmpty()) {
            requireNotNull(files.first().location) { "resource location is null" }
        } else {
            println("[$fullSqlFilename] WARN - test data SQL file does not exist or is empty, skipping execution")
            null
        }
    }

    /**
     * Gets the test class simple name.
     *
     * Uses reflection to obtain the final (concrete) class name of the running test instance.
     *
     * ## Core Flow
     * 1. Use `this::class.java.simpleName` to get the actual runtime class name of the current instance (not the declaring class)
     * 2. Within an inheritance chain, this returns the lowest subclass name
     *
     * @return the test class simple name
     */
    protected open fun getTestClassName(): String {
        return this::class.java.simpleName
    }

    @Resource
    protected lateinit var dataSource: DataSource

    /**
     * Extension hook invoked after test data loading completes.
     *
     * Does nothing by default; subclasses can use it to sync caches, clean up external state, etc.
     */
    protected open fun afterTestDataSetup() {
    }

    /**
     * Loads and executes the test data SQL file before each test method's transaction starts.
     *
     * Key fix — root causes of the previous issue:
     * 1. @BeforeEach runs inside the test method's transaction, but ResourceDatabasePopulator may not have executed correctly within that transaction
     * 2. When multiple test classes run together, although test methods are serial, each test class's @BeforeEach still runs,
     *    which can cause data interference between them
     *
     * Solution: use @BeforeTransaction instead of @BeforeEach
     * - @BeforeTransaction runs **before** the test transaction starts; data is committed to the database
     * - The test method can then see this data within its transaction
     * - Even though the data is not rolled back after the test (because it was committed outside the transaction),
     *   the SQL is re-executed before each test method, so the data stays clean
     *
     * Note: using @BeforeTransaction means data is committed outside the transaction and is not automatically rolled back after the test.
     * But since the SQL file is re-executed before each test method, data remains clean and free of interference.
     */
    @BeforeTransaction
    open fun setUpTestData() {
        val timestamp = System.currentTimeMillis()
        val threadName = Thread.currentThread().name
        val className = this::class.qualifiedName
        println(
            "[$timestamp] PID=${
                ProcessHandle.current().pid()
            } Thread=$threadName Class=$className - starting test data SQL execution"
        )

        val sqlPath = getTestDataSqlPath() ?: return
        val populator = ResourceDatabasePopulator()
        populator.addScript(ClassPathResource(sqlPath))
        populator.setSeparator(";")
        populator.setCommentPrefix("--")
        populator.setIgnoreFailedDrops(true)
        populator.setContinueOnError(false)

        try {
            // Use execute(dataSource) to run SQL outside the transaction; data is committed immediately
            // The test method can then see this data within its transaction
            // Although data is not rolled back after the test, the SQL is re-executed before each test method, so the data stays clean
            populator.execute(dataSource)
            afterTestDataSetup()
            println(
                "[$timestamp] PID=${
                    ProcessHandle.current().pid()
                } Thread=$threadName Class=$className - test data SQL executed successfully: $sqlPath"
            )
        } catch (e: Exception) {
            println(
                "[$timestamp] PID=${
                    ProcessHandle.current().pid()
                } Thread=$threadName Class=$className - test data SQL execution failed: $sqlPath, error: ${e.message}"
            )
            throw e
        }
    }

}
