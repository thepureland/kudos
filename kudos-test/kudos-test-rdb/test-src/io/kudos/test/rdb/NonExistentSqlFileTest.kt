package io.kudos.test.rdb

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

/**
 * Test case for when the test SQL file does not exist.
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class NonExistentSqlFileTest : RdbTestBase() {

    /**
     * Verifies that an exception is thrown when the SQL file does not exist.
     */
    @BeforeEach
    override fun setUpTestData() {
        val exception = assertThrows<IllegalStateException> {
            super.setUpTestData()
        }

        assertTrue(
            exception.message?.contains("Test data SQL file not found") == true,
            "Exception message should contain 'Test data SQL file not found', actual: ${exception.message}"
        )
    }

}
