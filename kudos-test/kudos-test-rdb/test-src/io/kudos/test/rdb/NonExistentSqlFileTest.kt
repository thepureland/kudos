package io.kudos.test.rdb

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

/**
 * 测试SQL文件不存在时的测试用例
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class NonExistentSqlFileTest : RdbTestBase() {

    /**
     * 测试SQL文件不存在时抛出异常
     */
    @BeforeEach
    override fun setUpTestData() {
        val exception = assertThrows<IllegalStateException> {
            super.setUpTestData()
        }

        assertTrue(
            exception.message?.contains("测试数据SQL文件不存在") == true,
            "异常消息应该包含'测试数据SQL文件不存在'，实际消息: ${exception.message}"
        )
    }

}
