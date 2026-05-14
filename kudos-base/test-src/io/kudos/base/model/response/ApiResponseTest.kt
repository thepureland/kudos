package io.kudos.base.model.response

import io.kudos.base.enums.impl.CommonErrorCodeEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ApiResponse 测试用例
 *
 * 覆盖 sealed 重构后的关键契约：
 * - 工厂方法仍按以前签名工作（生产端向后兼容）
 * - Success / Failure 各自的字段语义
 * - `when` 编译期穷尽性
 * - Failure 通过协变可赋值给任意 `ApiResponse<T>`
 * - 占位 timestamp 真的取当前时间
 *
 * @author K
 * @since 1.0.0
 */
internal class ApiResponseTest {

    // ============================================================
    // 工厂方法：success / fail
    // ============================================================

    @Test
    fun successFactoryProducesSuccessSubtype() {
        // 用 named arg 显式选 1-arg overload；裸 "hello" 在两个 overload 之间有歧义，
        // 会被 Kotlin 解析到 success(message, data=null)——这是 API 既有行为
        val resp: ApiResponse<String> = ApiResponse.success(data = "hello")
        assertTrue(resp is ApiResponse.Success<String>)
        assertEquals("hello", resp.data)
        assertTrue(resp.success)
        assertEquals(CommonErrorCodeEnum.SUCCESS.code, resp.code)
        assertEquals(CommonErrorCodeEnum.SUCCESS.displayText, resp.message)
    }

    @Test
    fun successFactoryOneArgGoesToMessageOverloadKnownBehavior() {
        // KNOWN BEHAVIOR：success(data) 和 success(message, data) 两个 overload 在
        // 单一 String 参数下歧义，Kotlin 会选 2-arg overload，把它当 message。
        // 若要传 data，请用 named arg `data = ...`。
        val resp: ApiResponse<String> = ApiResponse.success("hello")
        assertTrue(resp is ApiResponse.Success<String>)
        assertEquals("hello", resp.message, "裸 String 进的是 message，不是 data")
        assertNull(resp.data, "data 默认为 null")
    }

    @Test
    fun successFactoryWithoutDataAllowsNull() {
        val resp = ApiResponse.success<String>()
        assertTrue(resp is ApiResponse.Success<String>)
        assertNull(resp.data)
    }

    @Test
    fun successFactoryWithCustomMessage() {
        val resp = ApiResponse.success("已保存", "data-payload")
        assertTrue(resp is ApiResponse.Success<String>)
        assertEquals("已保存", resp.message)
        assertEquals("data-payload", resp.data)
        // 自定义消息时 code 仍是 SUCCESS
        assertEquals(CommonErrorCodeEnum.SUCCESS.code, resp.code)
    }

    @Test
    fun failFactoryProducesFailureSubtype() {
        val resp: ApiResponse<String> = ApiResponse.fail("400", "bad request")
        assertTrue(resp is ApiResponse.Failure)
        assertEquals("400", resp.code)
        assertEquals("bad request", resp.message)
        assertFalse(resp.success)
        assertNull(resp.errors)
    }

    @Test
    fun failFactoryWithErrors() {
        val errors = listOf(
            ErrorDetail(code = "REQUIRED", field = "name", message = "名称不能为空")
        )
        val resp = ApiResponse.fail<Any>("400", "validation failed", errors = errors)
        assertTrue(resp is ApiResponse.Failure)
        assertEquals(errors, resp.errors)
    }

    @Test
    fun failFactoryFromErrorCodeEnum() {
        val resp = ApiResponse.fail<String>(CommonErrorCodeEnum.BAD_REQUEST)
        assertTrue(resp is ApiResponse.Failure)
        assertEquals(CommonErrorCodeEnum.BAD_REQUEST.code, resp.code)
        assertEquals(CommonErrorCodeEnum.BAD_REQUEST.displayText, resp.message)
    }

    // ============================================================
    // success 标志固定值
    // ============================================================

    @Test
    fun successFlagIsAlwaysTrueOnSuccess() {
        assertTrue(ApiResponse.Success<String>(code = "200", data = "x").success)
        assertTrue(ApiResponse.success<String>().success)
        assertTrue(ApiResponse.success<String>("msg").success)
    }

    @Test
    fun successFlagIsAlwaysFalseOnFailure() {
        assertFalse(ApiResponse.Failure(code = "500", message = "boom").success)
        assertFalse(ApiResponse.fail<String>("400", "bad").success)
        assertFalse(ApiResponse.fail<String>(CommonErrorCodeEnum.BAD_REQUEST).success)
    }

    // ============================================================
    // sealed 编译期穷尽性
    // ============================================================

    @Test
    fun whenExpressionIsExhaustiveWithoutElse() {
        // 这个 when 没有 else 分支——能编译通过本身就说明 sealed 类型穷尽性生效
        val resp: ApiResponse<String> = ApiResponse.success(data = "x")
        val branch = when (resp) {
            is ApiResponse.Success -> "S:${resp.data}"
            is ApiResponse.Failure -> "F:${resp.code}"
        }
        assertEquals("S:x", branch)
    }

    @Test
    fun whenExpressionHandlesFailureBranch() {
        val resp: ApiResponse<String> = ApiResponse.fail("500", "boom")
        val branch = when (resp) {
            is ApiResponse.Success -> "S"
            is ApiResponse.Failure -> "F:${resp.code}"
        }
        assertEquals("F:500", branch)
    }

    // ============================================================
    // 协变：Failure 可赋值给任意 ApiResponse<T>
    // ============================================================

    @Test
    fun failureAssignableToTypedResponseViaCovariance() {
        // Failure : ApiResponse<Nothing>，out T 让它能成为 ApiResponse<String> 等
        val stringResp: ApiResponse<String> = ApiResponse.fail("400", "bad")
        val intResp: ApiResponse<Int> = ApiResponse.fail("400", "bad")
        val nestedResp: ApiResponse<List<Map<String, Any>>> = ApiResponse.fail("400", "bad")
        assertTrue(stringResp is ApiResponse.Failure)
        assertTrue(intResp is ApiResponse.Failure)
        assertTrue(nestedResp is ApiResponse.Failure)
    }

    // ============================================================
    // timestamp 默认值
    // ============================================================

    @Test
    fun timestampDefaultsToCurrentMillis() {
        val before = System.currentTimeMillis()
        val resp: ApiResponse<String> = ApiResponse.success("x")
        val after = System.currentTimeMillis()
        assertTrue(
            resp.timestamp in before..after,
            "timestamp ${resp.timestamp} 应在 [$before, $after] 区间内"
        )
    }

    // ============================================================
    // data class equals / copy 在各自子类上工作
    // ============================================================

    @Test
    fun successEqualsByAllPrimaryFields() {
        val a = ApiResponse.Success(code = "200", message = "ok", data = "x", timestamp = 1L)
        val b = ApiResponse.Success(code = "200", message = "ok", data = "x", timestamp = 1L)
        assertEquals(a, b)
    }

    @Test
    fun failureEqualsByAllPrimaryFields() {
        val a = ApiResponse.Failure(code = "400", message = "bad", errors = null, timestamp = 1L)
        val b = ApiResponse.Failure(code = "400", message = "bad", errors = null, timestamp = 1L)
        assertEquals(a, b)
    }

    @Test
    fun successCopyAllowsTraceIdInjection() {
        // 这条用例对应 GlobalResponseBodyHandler 的 traceId 回填流程
        val original = ApiResponse.Success(code = "200", message = "ok", data = "x")
        val withTrace = original.copy(traceId = "trace-001")
        assertEquals("trace-001", withTrace.traceId)
        assertEquals("x", withTrace.data, "copy 不影响其它字段")
    }

    @Test
    fun failureCopyAllowsTraceIdInjection() {
        val original = ApiResponse.Failure(code = "400", message = "bad")
        val withTrace = original.copy(traceId = "trace-002")
        assertEquals("trace-002", withTrace.traceId)
        assertEquals("400", withTrace.code)
    }
}
