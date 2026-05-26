package io.kudos.base.model.response

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.base.enums.impl.CommonErrorCodeEnum

/**
 * Unified-structure data returned to callers.
 *
 * Designed as a sealed class so that consumers can let the compiler force handling of both [Success] and
 * [Failure] in a `when` branch:
 *
 * ```kotlin
 * when (response) {
 *     is ApiResponse.Success -> handle(response.data)
 *     is ApiResponse.Failure -> handle(response.errors)
 * }
 * ```
 *
 * The factory methods [Companion.success] / [Companion.fail] keep their previous signatures (still declared
 * to return `ApiResponse<T>`), so producer-side construction code does not need to change.
 *
 * The JSON serialization shape is kept stable (combined with `explicitNulls = false` Json configuration):
 * - Success output: `{success, code, message, data, timestamp, traceId?}`
 * - Failure output: `{success, code, message, errors?, timestamp, traceId?}`
 *
 * @param T the business-data type
 * @author K
 * @author ChatGPT
 * @since 1.0.0
 */
sealed class ApiResponse<out T> {

    /** Whether the response is successful: always true for [Success], always false for [Failure]. */
    abstract val success: Boolean

    /** Response code (e.g. "200" / "400" / "USER_1001"). */
    abstract val code: String

    /** Response message (used for frontend display or development/debugging). */
    abstract val message: String?

    /** Response generation timestamp (milliseconds). */
    abstract val timestamp: Long

    /** Trace ID (optional). */
    abstract val traceId: String?

    /**
     * Successful response: carries business data [data].
     */
    data class Success<T>(
        override val code: String,
        override val message: String? = null,
        val data: T? = null,
        override val timestamp: Long = System.currentTimeMillis(),
        override val traceId: String? = null
    ) : ApiResponse<T>() {
        override val success: Boolean = true
    }

    /**
     * Failure response: carries an optional fine-grained error list [errors].
     *
     * The type parameter is fixed to [Nothing]; thanks to the outer `out T` covariance it can be assigned to
     * any `ApiResponse<T>`.
     */
    data class Failure(
        override val code: String,
        override val message: String? = null,
        val errors: List<ErrorDetail>? = null,
        override val timestamp: Long = System.currentTimeMillis(),
        override val traceId: String? = null
    ) : ApiResponse<Nothing>() {
        override val success: Boolean = false
    }

    companion object {

        /** Build a default successful response (uses [CommonErrorCodeEnum.SUCCESS]'s code and displayText). */
        fun <T> success(data: T? = null): ApiResponse<T> = Success(
            code = CommonErrorCodeEnum.SUCCESS.code,
            message = CommonErrorCodeEnum.SUCCESS.displayText,
            data = data
        )

        /** Build a successful response with a custom message. */
        fun <T> success(message: String, data: T? = null): ApiResponse<T> = Success(
            code = CommonErrorCodeEnum.SUCCESS.code,
            message = message,
            data = data
        )

        /**
         * Build a failure response (manually supplied code/message).
         *
         * The historical signature contained a `data: T?` parameter; it was removed during refactoring — [Failure]
         * does not carry business data, and in the legacy implementation `data` was never used by callers under
         * failure semantics. If an existing caller passes `null` as the third positional argument, it now binds to
         * the `errors` parameter of this signature, yielding equivalent behavior.
         */
        fun <T> fail(
            code: String,
            message: String,
            errors: List<ErrorDetail>? = null
        ): ApiResponse<T> = Failure(code, message, errors)

        /** Build a failure response driven by an error-code enum. */
        fun <T> fail(
            resultCode: IErrorCodeEnum,
            errors: List<ErrorDetail>? = null
        ): ApiResponse<T> = Failure(
            code = resultCode.code,
            message = resultCode.displayText,
            errors = errors
        )
    }
}
