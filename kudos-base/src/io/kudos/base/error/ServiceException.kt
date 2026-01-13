package io.kudos.base.error

import io.kudos.base.enums.ienums.IErrorCodeEnum
import java.util.*


/**
 * 服务层异常
 * 
 * 用于服务层业务逻辑中抛出的异常，支持错误码和参数化消息。
 * 
 * 核心特性：
 * 1. 错误码支持：使用IErrorCodeEnum错误码枚举，便于统一管理错误信息
 * 2. 参数化消息：支持在错误消息中使用占位符，动态替换参数值
 * 3. 异常链：支持包装底层异常，保留完整的异常堆栈信息
 * 4. 日志控制：支持控制是否自动记录异常日志
 * 
 * 构造函数：
 * - errorCode：使用错误码创建异常，自动格式化错误消息
 * - errorCode + args：使用错误码和参数创建异常，支持消息参数化
 * - errorCode + cause：使用错误码和原因异常创建异常，保留异常链
 * - errorCode + cause + args：完整参数版本，支持错误码、原因和参数
 * - errorCode + cause + log：支持控制是否记录日志
 * 
 * 错误码机制：
 * - 错误码包含错误代码和错误描述（trans）
 * - 错误描述支持MessageFormat格式，可以使用{0}、{1}等占位符
 * - 参数通过args数组传入，自动替换占位符
 * 
 * 使用场景：
 * - 业务逻辑验证失败
 * - 数据操作异常
 * - 权限检查失败
 * - 业务规则违反
 * 
 * 注意事项：
 * - 继承自CustomRuntimeException，自动支持消息格式化和堆栈控制
 * - 错误码的trans字段会作为异常消息
 * - 参数会保存到params属性中，便于后续处理
 * 
 * @since 1.0.0
 */
class ServiceException : CustomRuntimeException {

    var errorCode: IErrorCodeEnum? = null

    var params: Array<Any?>? = null
        private set


    constructor(errorCode: IErrorCodeEnum) {
        this.errorCode = errorCode
        resolveException(errorCode)
    }

    constructor(errorCode: IErrorCodeEnum, vararg args: Any?) {
        this.errorCode = errorCode
        this.params = Arrays.stream<Any?>(args).toArray()
        resolveException(errorCode, *args)
    }

    constructor(errorCode: IErrorCodeEnum, cause: Throwable, vararg args: Any?) {
        this.errorCode = errorCode
        resolveCauseException(cause, errorCode.trans, args)
    }

    constructor(errorCode: IErrorCodeEnum, cause: Throwable, log: Boolean, vararg args: Any?) {
        this.errorCode = errorCode
        resolveCauseException(cause, log, errorCode.trans, args)
    }

    constructor(errorCode: IErrorCodeEnum, ex: Throwable) {
        this.errorCode = errorCode
        resolveCauseException(ex, errorCode.trans)
    }

    companion object {
        private const val serialVersionUID = 1620536616422855704L
    }

}
