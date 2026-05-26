package io.kudos.ability.cache.interservice.provider.web

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse

/**
 * Request wrapper used for cache handling.
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
