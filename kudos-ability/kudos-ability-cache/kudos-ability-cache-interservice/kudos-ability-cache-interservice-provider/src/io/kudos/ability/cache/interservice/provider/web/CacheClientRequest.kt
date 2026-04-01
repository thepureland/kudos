package io.kudos.ability.cache.interservice.provider.web

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse

/**
 * 请求的request对象
 * 用于设置缓存
 */
class CacheClientRequest(
    request: HttpServletRequest?,
    private var servletResponse: HttpServletResponse? = null,
) : HttpServletRequestWrapper(request) {

    fun getServletResponse(): HttpServletResponse? = servletResponse

    fun setServletResponse(servletResponse: HttpServletResponse?) {
        this.servletResponse = servletResponse
    }
}
