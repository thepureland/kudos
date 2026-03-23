package io.kudos.base.model.response

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.base.enums.impl.CommonErrorCodeEnum

/**
 * 用于返回给调用方的统一结构的数据。
 *
 * 设计目标：
 * 1. 成功与失败统一结构
 * 2. 便于前端统一处理响应
 * 3. 支持返回业务数据
 * 4. 支持返回结构化错误明细
 * 5. 支持链路追踪
 *
 * @param T 业务数据类型
 * @author K
 * @author ChatGPT
 * @since 1.0.0
 */
data class ApiResponse<T>(

    /**
     * 是否成功
     *
     * true 表示本次请求处理成功；
     * false 表示本次请求处理失败。
     *
     * 前端通常可先根据该字段快速判断请求结果。
     */
    val success: Boolean,

    /**
     * 响应码
     *
     * 用于标识本次请求结果的机器可识别编码。
     *
     * 例如：
     * - 200：成功
     * - 400：参数错误
     * - 500：系统异常
     * - USER_1001：用户名已存在
     *
     * 该字段主要用于前端分支处理、日志归类、接口契约约定等场景。
     */
    val code: String,

    /**
     * 响应消息
     *
     * 用于描述本次请求结果的文字说明，
     * 通常用于前端提示用户，或供开发调试查看。
     *
     * 例如：
     * - success
     * - 参数错误
     * - 用户不存在
     * - 保存成功
     */
    val message: String,

    /**
     * 响应数据
     *
     * 用于承载本次请求返回的业务数据。
     *
     * 常见情况：
     * - 查询详情时为对象
     * - 查询列表时为列表
     * - 分页查询时为分页对象
     * - 删除、保存成功但无额外返回内容时可为 null
     */
    val data: T? = null,

    /**
     * 响应时间戳
     *
     * 表示本次响应生成时的时间，通常为毫秒级时间戳。
     *
     * 用途：
     * - 便于排查问题
     * - 便于前端记录请求响应时间
     * - 便于日志或监控分析
     */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * 错误明细列表
     *
     * 用于承载比 message 更细粒度的错误信息。
     *
     * 常见适用场景：
     * - 参数校验失败
     * - 批量操作部分失败
     * - 字段级错误提示
     * - 复杂业务校验失败
     *
     * 例如：
     * - 哪个字段错误
     * - 哪一行导入失败
     * - 哪条记录处理失败
     *
     * 正常成功响应时通常为 null。
     */
    val errors: List<ErrorDetail>? = null,

    /**
     * 链路追踪 ID
     *
     * 用于标识本次请求在日志链路中的唯一追踪标识。
     *
     * 适用于：
     * - 微服务链路追踪
     * - 日志排查
     * - 前后端协同定位问题
     *
     * 如果项目未启用链路追踪，可为空。
     */
    val traceId: String? = null
) {

    companion object {

        /**
         * 构造成功响应
         *
         * 使用默认成功响应码和默认成功消息。
         *
         * 适用于：
         * - 普通查询成功
         * - 保存成功但无需自定义消息
         * - 删除成功但仅需返回数据
         *
         * @param data 业务数据，可为空
         * @return 统一成功响应对象
         */
        fun <T> success(data: T? = null): ApiResponse<T> =
            ApiResponse(
                success = true,
                code = CommonErrorCodeEnum.SUCCESS.code,
                message = CommonErrorCodeEnum.SUCCESS.displayText,
                data = data
            )

        /**
         * 构造成功响应
         *
         * 使用默认成功响应码，但允许自定义成功消息。
         *
         * 适用于：
         * - 新增成功
         * - 修改成功
         * - 删除成功
         * - 自定义成功提示文案
         *
         * @param message 自定义成功消息
         * @param data 业务数据，可为空
         * @return 统一成功响应对象
         */
        fun <T> success(message: String, data: T? = null): ApiResponse<T> =
            ApiResponse(
                success = true,
                code = CommonErrorCodeEnum.SUCCESS.code,
                message = message,
                data = data
            )

        /**
         * 构造失败响应
         *
         * 由调用方直接传入错误码和错误消息。
         *
         * 适用于：
         * - 临时错误返回
         * - 非枚举型错误码场景
         * - 需要手动指定 code 和 message 的场景
         *
         * @param code 错误码
         * @param message 错误消息
         * @param data 附加返回数据，可为空
         * @param errors 错误明细列表，可为空
         * @return 统一失败响应对象
         */
        fun <T> fail(
            code: String,
            message: String,
            data: T? = null,
            errors: List<ErrorDetail>? = null
        ): ApiResponse<T> =
            ApiResponse(
                success = false,
                code = code,
                message = message,
                data = data,
                errors = errors
            )

        /**
         * 构造失败响应
         *
         * 由错误码枚举对象生成失败响应，
         * 自动读取枚举中的 code 和 displayText 作为响应码和响应消息。
         *
         * 适用于：
         * - 统一业务异常返回
         * - 通用错误码返回
         * - 前后端约定固定错误码的场景
         *
         * @param resultCode 错误码枚举对象
         * @param data 附加返回数据，可为空
         * @param errors 错误明细列表，可为空
         * @return 统一失败响应对象
         */
        fun <T> fail(
            resultCode: IErrorCodeEnum,
            data: T? = null,
            errors: List<ErrorDetail>? = null
        ): ApiResponse<T> {
            return ApiResponse(
                success = false,
                code = resultCode.code,
                message = resultCode.displayText,
                data = data,
                errors = errors
            )
        }

    }

}
