package io.kudos.test.rdb

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import kotlin.test.Test
import kotlin.test.assertNull

/**
 * Test case for when the test data SQL file does not exist.
 *
 * Current contract (see [SqlTestBase.getTestDataSqlPath]): a missing fixture SQL file is NOT an error —
 * it only logs a warning and returns null, and [SqlTestBase.setUpTestData] skips execution silently.
 * This supports tests whose fixtures live entirely in `@Sql` annotations or in code.
 *
 * Note: this class previously had no `@Test` method (so it never executed) and asserted a
 * "throw IllegalStateException" behavior that had long been removed.
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class NonExistentSqlFileTest : RdbTestBase() {

    /**
     * There is intentionally no `sql/h2/NonExistentSqlFileTest.sql` on the classpath.
     * Reaching this test method at all proves that [SqlTestBase.setUpTestData] (run via
     * `@BeforeTransaction`) tolerates the missing file without throwing; the explicit
     * assertion documents that the resolved path is null in that case.
     */
    @Test
    fun missingSqlFileIsSkippedWithoutError() {
        assertNull(getTestDataSqlPath(), "missing fixture SQL file should resolve to null instead of throwing")
    }

}
