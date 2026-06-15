package io.kudos.test.rdb

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure (container-free) unit tests for the parts of [SqlTestBase] and its subclasses that do
 * not require a DataSource, Spring context, Redis, or Docker.
 *
 * The DataSource-backed paths ([SqlTestBase.getTestDataSqlPath], [SqlTestBase.setUpTestData])
 * are covered by the container-based test classes ([RdbTestBaseTest], [RdbAndRedisCacheTestBaseTest],
 * [NonExistentSqlFileTest]); here we cover the data-source-independent hooks:
 *
 * - [SqlTestBase.getTestClassName]: reflection-based simple-name resolution returns the concrete
 *   runtime subclass name, not the declaring class name.
 * - [SqlTestBase.afterTestDataSetup]: default no-op completes without side effects.
 * - [RdbAndRedisCacheTestBase.afterTestDataSetup]: delegates to [CacheTestResetSupport], which is a
 *   safe no-op when no Spring context / cache beans are present (exactly the case in this pure test JVM).
 *
 * @author K
 * @since 1.0.0
 */
internal class SqlTestBasePureLogicTest {

    /** Concrete subclass that exposes the protected hooks for direct assertion. */
    private class ProbeSqlTestBase : SqlTestBase() {
        fun callGetTestClassName(): String = getTestClassName()
        fun callAfterTestDataSetup() = afterTestDataSetup()
    }

    /** Concrete subclass of the cache base to exercise its overridden afterTestDataSetup. */
    private class ProbeRdbAndRedisCacheTestBase : RdbAndRedisCacheTestBase() {
        fun callAfterTestDataSetup() = afterTestDataSetup()
    }

    @Test
    fun getTestClassNameReturnsConcreteRuntimeClassName() {
        val probe = ProbeSqlTestBase()
        assertEquals(
            "ProbeSqlTestBase",
            probe.callGetTestClassName(),
            "getTestClassName must resolve the lowest concrete subclass simple name via reflection"
        )
    }

    @Test
    fun defaultAfterTestDataSetupIsNoOp() {
        // Must complete normally without requiring any external state.
        ProbeSqlTestBase().callAfterTestDataSetup()
    }

    @Test
    fun cacheBaseAfterTestDataSetupIsSafeWithoutSpringContext() {
        // RdbAndRedisCacheTestBase.afterTestDataSetup -> CacheTestResetSupport.resetRedisAndApplicationCaches,
        // which must be a safe no-op when there is no Spring context and no cache beans available.
        ProbeRdbAndRedisCacheTestBase().callAfterTestDataSetup()
    }
}
