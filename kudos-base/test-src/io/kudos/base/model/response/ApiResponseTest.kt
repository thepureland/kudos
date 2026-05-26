package io.kudos.base.model.response

import io.kudos.base.enums.impl.CommonErrorCodeEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ApiResponse test cases.
 *
 * Covers the key contracts after the sealed refactor:
 * - Factory methods still work with the previous signatures (producer-side backward compatibility)
 * - Field semantics of Success / Failure
 * - Compile-time exhaustiveness of `when`
 * - Failure is assignable to any `ApiResponse<T>` via covariance
 * - Placeholder timestamp actually picks up the current time
 *
 * @author K
 * @since 1.0.0
 */
internal class ApiResponseTest {

    // ============================================================
    // Factory methods: success / fail
    // ============================================================

    @Test
    fun successFactoryProducesSuccessSubtype() {
        // Use a named arg to explicitly pick the 1-arg overload; bare "hello" is ambiguous
        // between the two overloads and Kotlin resolves to success(message, data=null) - this is the existing API behavior.
        val resp: ApiResponse<String> = ApiResponse.success(data = "hello")
        assertTrue(resp is ApiResponse.Success<String>)
        assertEquals("hello", resp.data)
        assertTrue(resp.success)
        assertEquals(CommonErrorCodeEnum.SUCCESS.code, resp.code)
        assertEquals(CommonErrorCodeEnum.SUCCESS.displayText, resp.message)
    }

    @Test
    fun successFactoryOneArgGoesToMessageOverloadKnownBehavior() {
        // KNOWN BEHAVIOR: success(data) and success(message, data) are ambiguous with a single String
        // argument; Kotlin picks the 2-arg overload and treats it as message.
        // To pass data, use the named arg `data = ...`.
        val resp: ApiResponse<String> = ApiResponse.success("hello")
        assertTrue(resp is ApiResponse.Success<String>)
        assertEquals("hello", resp.message, "Bare String becomes message, not data")
        assertNull(resp.data, "data defaults to null")
    }

    @Test
    fun successFactoryWithoutDataAllowsNull() {
        val resp = ApiResponse.success<String>()
        assertTrue(resp is ApiResponse.Success<String>)
        assertNull(resp.data)
    }

    @Test
    fun successFactoryWithCustomMessage() {
        val resp = ApiResponse.success("Saved", "data-payload")
        assertTrue(resp is ApiResponse.Success<String>)
        assertEquals("Saved", resp.message)
        assertEquals("data-payload", resp.data)
        // With a custom message, code is still SUCCESS
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
            ErrorDetail(code = "REQUIRED", field = "name", message = "name must not be empty")
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
    // success flag fixed value
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
    // Sealed compile-time exhaustiveness
    // ============================================================

    @Test
    fun whenExpressionIsExhaustiveWithoutElse() {
        // This `when` has no else branch - successful compilation alone shows sealed-type exhaustiveness takes effect
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
    // Covariance: Failure assignable to any ApiResponse<T>
    // ============================================================

    @Test
    fun failureAssignableToTypedResponseViaCovariance() {
        // Failure : ApiResponse<Nothing>; the `out T` lets it become ApiResponse<String> etc.
        val stringResp: ApiResponse<String> = ApiResponse.fail("400", "bad")
        val intResp: ApiResponse<Int> = ApiResponse.fail("400", "bad")
        val nestedResp: ApiResponse<List<Map<String, Any>>> = ApiResponse.fail("400", "bad")
        assertTrue(stringResp is ApiResponse.Failure)
        assertTrue(intResp is ApiResponse.Failure)
        assertTrue(nestedResp is ApiResponse.Failure)
    }

    // ============================================================
    // timestamp default
    // ============================================================

    @Test
    fun timestampDefaultsToCurrentMillis() {
        val before = System.currentTimeMillis()
        val resp: ApiResponse<String> = ApiResponse.success("x")
        val after = System.currentTimeMillis()
        assertTrue(
            resp.timestamp in before..after,
            "timestamp ${resp.timestamp} should be within [$before, $after]"
        )
    }

    // ============================================================
    // data class equals / copy work on each subclass
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
        // This case corresponds to GlobalResponseBodyHandler's traceId backfill flow
        val original = ApiResponse.Success(code = "200", message = "ok", data = "x")
        val withTrace = original.copy(traceId = "trace-001")
        assertEquals("trace-001", withTrace.traceId)
        assertEquals("x", withTrace.data, "copy should not affect other fields")
    }

    @Test
    fun failureCopyAllowsTraceIdInjection() {
        val original = ApiResponse.Failure(code = "400", message = "bad")
        val withTrace = original.copy(traceId = "trace-002")
        assertEquals("trace-002", withTrace.traceId)
        assertEquals("400", withTrace.code)
    }
}
