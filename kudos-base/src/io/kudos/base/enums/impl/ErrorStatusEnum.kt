package io.kudos.base.enums.impl

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * 错误状态码枚举
 *
 * @author jason
 * @author hanson
 * @author K
 * @since 1.0.0
 */
enum class ErrorStatusEnum(
    override val code: String,
    override val defaultDisplayText: String,
    val url: String?
) : IErrorCodeEnum {

    //原生状态 @see HttpStatus.SC_BAD_GATEWAY
    SC_FORBIDDEN("403", "SC_FORBIDDEN", "/errors/403"),
    SC_NOT_FOUND("404", "SC_NOT_FOUND", "/errors/404"),
    SC_REQUEST_FREQUENTLY("503", "SC_REQUEST_FREQUENTLY", "/errors/503"),  //請求頻繁
    SC_SESSION_EXPIRE("600", "SC_SESSION_EXPIRE", "/"),  //session过期
    SC_PRIVILEGE("601", "SC_PRIVILEGE", ""),  //需要安全密码
    SC_SERVICE_BUSY("602", "SC_SERVICE_BUSY", "/errors/602"),  //服务忙
    SC_DOMAIN_NO_EXIST("603", "SC_DOMAIN_NO_EXIST", "/errors/603"),  //域名不存在
    SC_DOMAIN_TEMP_TIMEOUT("604", "SC_DOMAIN_TEMP_TIMEOUT", "/errors/604"),  //临时域名过期
    SC_IP_CONFINE("605", "SC_IP_CONFINE", "/errors/605"),  //IP被限制
    SC_KICK_OUT("606", "SC_KICK_OUT", "/errors/606"),  //被强制踢出
    SC_SITE_MAINTAIN("607", "SC_SITE_MAINTAIN", "/errors/607"),  //站点维护
    SC_REPEAT_REQUEST("608", "SC_REPEAT_REQUEST", "/errors/608"),  //重复请求
    SC_SITE_DISABLED("609", "SITE_DISABLED", ""),  //站点不存在
    SC_NOT_TENANT_ENABLE("700", "SC_NOT_TENANT_ENABLE", ""),  //不允许的租户访问
    SC_MODULE_MAINTAIN("610", "SC_MODULE_MAINTAIN", "/errors/610"); //模块维护


    val status: Int
        get() = this.code.toInt()

    override val i18nKeyPrefix: String
        get() = ""

}
