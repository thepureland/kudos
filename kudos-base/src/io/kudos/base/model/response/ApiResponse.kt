package io.kudos.base.model.response

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.base.enums.impl.CommonErrorCodeEnum

/**
 * 用于返回给调用方的统一结构的数据。
 *
 * 设计为 sealed class，消费端在 `when` 分支里可以让编译器强制处理 [Success] 与 [Failure]：
 *
 * ```kotlin
 * when (response) {
 *     is ApiResponse.Success -> handle(response.data)
 *     is ApiResponse.Failure -> handle(response.errors)
 * }
 * ```
 *
 * 工厂方法 [Companion.success] / [Companion.fail] 仍按以前的签名提供（返回值仍声明为
 * `ApiResponse<T>`），生产端构造代码无需修改。
 *
 * JSON 序列化形态保持稳定（配合 `explicitNulls = false` 的 Json 配置）：
 * - Success 输出：`{success, code, message, data, timestamp, traceId?}`
 * - Failure 输出：`{success, code, message, errors?, timestamp, traceId?}`
 *
 * @param T 业务数据类型
 * @author K
 * @author ChatGPT
 * @since 1.0.0
 */
sealed class ApiResponse<out T> {

    /** 是否成功：[Success] 恒为 true，[Failure] 恒为 false */
    abstract val success: Boolean

    /** 响应码（如 "200" / "400" / "USER_1001"） */
    abstract val code: String

    /** 响应消息（用于前端提示或开发调试） */
    abstract val message: String?

    /** 响应生成时间戳（毫秒） */
    abstract val timestamp: Long

    /** 链路追踪 ID（可选） */
    abstract val traceId: String?

    /**
     * 成功响应：携带业务数据 [data]。
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
     * 失败响应：携带可选的细粒度错误列表 [errors]。
     *
     * 类型参数固定为 [Nothing]，借助外层 `out T` 的协变可赋值给任意 `ApiResponse<T>`。
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

        /** 构造默认成功响应（使用 [CommonErrorCodeEnum.SUCCESS] 的 code 与 displayText） */
        fun <T> success(data: T? = null): ApiResponse<T> = Success(
            code = CommonErrorCodeEnum.SUCCESS.code,
            message = CommonErrorCodeEnum.SUCCESS.displayText,
            data = data
        )

        /** 构造带自定义消息的成功响应 */
        fun <T> success(message: String, data: T? = null): ApiResponse<T> = Success(
            code = CommonErrorCodeEnum.SUCCESS.code,
            message = message,
            data = data
        )

        /**
         * 构造失败响应（手动传 code/message）。
         *
         * 历史签名包含一个 `data: T?` 参数，重构后移除——[Failure] 不携带业务数据，
         * 旧实现里 `data` 在失败语义下从未被业务使用过。如有 caller 用第三个位置参数
         * 传 `null`，它会自动绑定到本签名的 `errors`，行为等价。
         */
        fun <T> fail(
            code: String,
            message: String,
            errors: List<ErrorDetail>? = null
        ): ApiResponse<T> = Failure(code, message, errors)

        /** 构造失败响应（错误码枚举驱动） */
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
