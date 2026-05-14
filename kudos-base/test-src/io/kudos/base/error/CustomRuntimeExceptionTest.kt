package io.kudos.base.error

import io.kudos.base.enums.ienums.IErrorCodeEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * CustomRuntimeException测试用例
 *
 * @author AI: ChatGPT
 * @since 1.0.0
 */
internal class CustomRuntimeExceptionTest {

    @Test
    fun testConstructorWithCauseHavingNullMessage() {
        val cause = NullPointerException()
        val exception = CustomRuntimeException(cause)

        assertNotNull(exception.message)
        assertEquals(NullPointerException::class.java.name, exception.message)
    }

    @Test
    fun testErrorCodeMessageFormattingWithArgs() {
        val errorCode = TestErrorCode("E001", "用户{0}操作失败({1})")
        val exception = ServiceException(errorCode, false, "alice", 42)

        assertEquals("用户alice操作失败(42)", exception.message)
    }

    @Test
    fun testErrorCodeMessageFormattingWithCauseAndArgs() {
        val errorCode = TestErrorCode("E002", "调用{0}时出错")
        val cause = RuntimeException("inner")
        val exception = ServiceException(errorCode, cause, "minio")

        assertEquals("调用minio时出错", exception.message)
    }

    @Test
    fun testErrorCodeMessageFormattingWithCauseLogAndArgs() {
        val errorCode = TestErrorCode("E003", "操作{0}失败，原因码{1}")
        val cause = RuntimeException("inner")
        val exception = ServiceException(errorCode, cause, false, "delete", 7)

        assertEquals("操作delete失败，原因码7", exception.message)
    }

    /**
     * 精简模式下应保留业务调用方的栈帧，且总数不超过 20。
     * 关键验证：旧实现固定 5 帧时，深层业务调用会被切掉；新实现 20 帧应能保留它们。
     */
    @Test
    fun testTruncatedStackPreservesBusinessCaller() {
        val errorCode = TestErrorCode("E100", "test")
        val exception = createDeeplyNested(errorCode, depth = 8)
        val trace = exception.stackTrace

        assertTrue(trace.isNotEmpty(), "精简模式不应清空堆栈")
        assertTrue(trace.size <= 20, "精简模式至多保留 20 帧，实际: ${trace.size}")
        // 8 层深的辅助函数链以及最外层测试方法都应在精简栈中可见
        val classNames = trace.map { it.className }
        assertTrue(
            classNames.contains(CustomRuntimeExceptionTest::class.java.name),
            "精简栈应包含业务调用方，实际栈: $classNames"
        )
    }

    private fun createDeeplyNested(errorCode: IErrorCodeEnum, depth: Int): ServiceException {
        return if (depth <= 0) ServiceException(errorCode, false)
        else createDeeplyNested(errorCode, depth - 1)
    }

    private data class TestErrorCode(
        override val code: String,
        override val defaultDisplayText: String,
        override val i18nKeyPrefix: String = ""
    ) : IErrorCodeEnum
}
