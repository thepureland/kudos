package io.kudos.ability.web.springmvc.init

import org.soul.base.ienums.ICodeEnum


/**
 * springboot所支持的内嵌servlet容器枚举
 *
 * @author K
 * @since 5.0.0
 */
enum class ServletServerEnum(private val code: String, private val trans: String) : ICodeEnum {

    TOMCAT("tomcat", "Tomcat"),
    JETTY("jetty", "Jetty"),
    UNDERTOW("undertow", "Undertow");

    override fun getCode(): String {
        return code
    }

    override fun getTrans(): String {
        return trans
    }
}