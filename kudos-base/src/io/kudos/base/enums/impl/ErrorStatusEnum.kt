package io.kudos.base.enums.impl

import io.kudos.base.enums.ienums.IErrorCodeEnum

/**
 * Error status code enum.
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

    // native status @see HttpStatus.SC_BAD_GATEWAY
    SC_FORBIDDEN("403", "SC_FORBIDDEN", "/errors/403"),
    SC_NOT_FOUND("404", "SC_NOT_FOUND", "/errors/404"),
    SC_REQUEST_FREQUENTLY("503", "SC_REQUEST_FREQUENTLY", "/errors/503"),  // requests too frequent
    SC_SESSION_EXPIRE("600", "SC_SESSION_EXPIRE", "/"),  // session expired
    SC_PRIVILEGE("601", "SC_PRIVILEGE", ""),  // security password required
    SC_SERVICE_BUSY("602", "SC_SERVICE_BUSY", "/errors/602"),  // service busy
    SC_DOMAIN_NO_EXIST("603", "SC_DOMAIN_NO_EXIST", "/errors/603"),  // domain does not exist
    SC_DOMAIN_TEMP_TIMEOUT("604", "SC_DOMAIN_TEMP_TIMEOUT", "/errors/604"),  // temporary domain expired
    SC_IP_CONFINE("605", "SC_IP_CONFINE", "/errors/605"),  // IP restricted
    SC_KICK_OUT("606", "SC_KICK_OUT", "/errors/606"),  // forcibly kicked out
    SC_SITE_MAINTAIN("607", "SC_SITE_MAINTAIN", "/errors/607"),  // site under maintenance
    SC_REPEAT_REQUEST("608", "SC_REPEAT_REQUEST", "/errors/608"),  // duplicate request
    SC_SITE_DISABLED("609", "SITE_DISABLED", ""),  // site does not exist
    SC_NOT_TENANT_ENABLE("700", "SC_NOT_TENANT_ENABLE", ""),  // tenant access not allowed
    SC_MODULE_MAINTAIN("610", "SC_MODULE_MAINTAIN", "/errors/610"); // module under maintenance


    val status: Int
        get() = this.code.toInt()

    override val i18nKeyPrefix: String
        get() = ""

}
