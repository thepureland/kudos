package io.kudos.base.enums.impl

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.base.enums.ienums.IModuleEnum

/**
 * 公共错误码枚举
 *
 * @author K
 * @since 1.0.0
 */
enum class CommonErrorCodeEnum(
    /** 错误码 */
    override val code: String,

    /** 原始错误描述 */
    override val rawTrans: String,

    /** 是否打印完整堆栈 */
    override val printAllStackTrace: Boolean = false,

    /** 国际化key的前缀 */
    override val i18nKeyPrefix: String = "sys.error-msg.default",
) : IErrorCodeEnum {

    /** 请求成功 */
    SUCCESS("200", "操作成功"),

    /** 参数错误 */
    BAD_REQUEST("400", "请求有误，请检查后重试"),

    /** 未登录 */
    UNAUTHORIZED("401", "请先登录"),

    /** 无权限 */
    FORBIDDEN("403", "抱歉，您暂无权限执行此操作"),

    /** 资源不存在 */
    NOT_FOUND("404", "抱歉，您访问的内容不存在"),

    /** 请求方法不支持 */
    METHOD_NOT_ALLOWED("405", "当前请求方式不支持，请换一种方式再试"),

    /** 参数校验失败 */
    VALIDATION_ERROR("4001", "您填写的信息有误，请检查后重试"),

    /** 业务处理失败 */
    BUSINESS_ERROR("4002", "操作未完成，请稍后再试"),

    /** 系统异常 */
    SYSTEM_ERROR("500", "系统开小差了，请稍后再试")

}
