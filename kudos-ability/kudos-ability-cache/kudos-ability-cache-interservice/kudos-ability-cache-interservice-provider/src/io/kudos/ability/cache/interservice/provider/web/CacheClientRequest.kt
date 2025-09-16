package io.kudos.ability.cache.interservice.provider.web

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse

/**
 * 请求的request对象
 * 用于设置缓存
 */
class CacheClientRequest : HttpServletRequestWrapper {
    private var servletResponse: HttpServletResponse? = null

    constructor(request: HttpServletRequest?) : super(request)

    constructor(request: HttpServletRequest?, servletResponse: HttpServletResponse?) : super(request) {
        this.servletResponse = servletResponse
    }

    fun getServletResponse(): HttpServletResponse? {
        return servletResponse
    }

    fun setServletResponse(servletResponse: HttpServletResponse?) {
        this.servletResponse = servletResponse
    }
}
