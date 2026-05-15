package io.kudos.context.retry

import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * RetryConfig 测试用例
 *
 * 覆盖路径解析优先级、空白处理、跨平台默认值。
 *
 * 注意：[RetryConfig.baseFailedDataPath] 是 `by lazy`，整个 JVM 生命周期只解析一次。
 * 测试时只能验证 [RetryConfig.resolveBasePath] 这个 internal 方法（每次调用都重读优先级）。
 *
 * @author K
 * @since 1.0.0
 */
internal class RetryConfigTest {

    @AfterTest
    fun cleanup() {
        System.clearProperty(RetryConfig.SYS_PROP_BASE_PATH)
    }

    @Test
    fun resolveBasePathPicksSystemPropertyWhenSet() {
        System.setProperty(RetryConfig.SYS_PROP_BASE_PATH, "/custom/path")
        assertEquals("/custom/path", RetryConfig.resolveBasePath())
    }

    @Test
    fun resolveBasePathFallsBackToTmpDirWhenNothingSet() {
        System.clearProperty(RetryConfig.SYS_PROP_BASE_PATH)
        // 环境变量在测试进程里通常没设，所以应回到 tmpdir 兜底
        val resolved = RetryConfig.resolveBasePath()
        val tmp = System.getProperty("java.io.tmpdir").trimEnd('/', '\\')
        assertEquals(tmp + File.separator + "kudos-failed-data", resolved)
    }

    @Test
    fun resolveBasePathIgnoresBlankSystemProperty() {
        // 空白字符串不应被识别为有效配置
        System.setProperty(RetryConfig.SYS_PROP_BASE_PATH, "   ")
        val resolved = RetryConfig.resolveBasePath()
        assertTrue(resolved.endsWith("kudos-failed-data"), "空白应被忽略，落到默认值：$resolved")
    }

    @Test
    fun pathForUsesProvidedServiceCode() {
        System.setProperty(RetryConfig.SYS_PROP_BASE_PATH, "/base")
        // baseFailedDataPath 是 lazy，已被首次访问冻结，所以这里 pathFor 用的是首次解析的值。
        // 测试只检查 pathFor 的拼接逻辑——使用 resolveBasePath 而非 baseFailedDataPath 来构造期望值。
        val expected = RetryConfig.baseFailedDataPath + File.separator + "sys"
        assertEquals(expected, RetryConfig.pathFor("sys"))
    }

    @Test
    fun pathForFallsBackToDefaultForNullOrBlankServiceCode() {
        val withNull = RetryConfig.pathFor(null)
        val withBlank = RetryConfig.pathFor("   ")
        val withEmpty = RetryConfig.pathFor("")

        assertTrue(withNull.endsWith(File.separator + "default"), "null serviceCode 应用 'default'")
        assertTrue(withBlank.endsWith(File.separator + "default"), "空白 serviceCode 应用 'default'")
        assertTrue(withEmpty.endsWith(File.separator + "default"), "空字符串 serviceCode 应用 'default'")
    }

    @Test
    fun pathForKeepsValidServiceCodeAsIs() {
        val result = RetryConfig.pathFor("payment-svc")
        assertTrue(result.endsWith(File.separator + "payment-svc"))
        assertNotNull(result)
    }
}
