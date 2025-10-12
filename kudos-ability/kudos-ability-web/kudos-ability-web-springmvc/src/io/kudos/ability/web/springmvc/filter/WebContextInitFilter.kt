package io.kudos.ability.web.springmvc.filter

import io.kudos.ability.web.springmvc.support.getBrowserInfo
import io.kudos.ability.web.springmvc.support.getOsInfo
import io.kudos.ability.web.springmvc.support.getRemoteIp
import io.kudos.context.core.ClientInfo
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest


/**
 * Web上下文初始化过滤器接口
 *
 * @author K
 * @since 1.0.0
 */
open class WebContextInitFilter: IWebContextInitFilter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val context = KudosContext()

        // session
        val session = (request as HttpServletRequest).session
        session.attributeNames.asIterator().forEach { name ->
            val value = session.getAttribute(name)
            context.addSessionAttributes(Pair(name, value))
        }

        // cookie
        request.cookies?.forEach { cookie ->
            context.addCookieAttributes(Pair(cookie.name, cookie.value))
        }

        // header
        request.headerNames.asIterator().forEach { name ->
            val value = request.getHeader(name)
            context.addHeaderAttributes(Pair(name, value))
        }

        // client info
        val clientInfoBuilder = ClientInfo.Builder()
        clientInfoBuilder.ip(request.getRemoteIp())
        clientInfoBuilder.domain(request.serverName)
        clientInfoBuilder.url(request.requestURI)
        clientInfoBuilder.params(request.parameterMap)
        clientInfoBuilder.browser(request.getBrowserInfo())
        clientInfoBuilder.os(request.getOsInfo())
        clientInfoBuilder.requestReferer(request.getHeader("referer"))
        clientInfoBuilder.locale(request.locale)
//        clientInfoBuilder.timeZone() //TODO
        context.clientInfo = ClientInfo(clientInfoBuilder)


        // 初始化上下文
        KudosContextHolder.set(context)

        chain.doFilter(request, response)
    }


}