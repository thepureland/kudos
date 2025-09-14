package io.kudos.base.error

import io.kudos.base.enums.ienums.IErrorCodeEnum
import java.util.*


/**
 * 服务层抛出的异常
 *
 * @author K
 * @since 1.0.0
 */
class ServiceException : CustomRuntimeException {

    private var errorCode: IErrorCodeEnum? = null

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

    fun getErrorCode(): IErrorCodeEnum? {
        return errorCode
    }

    companion object {
        private const val serialVersionUID = 1620536616422855704L
    }

}
