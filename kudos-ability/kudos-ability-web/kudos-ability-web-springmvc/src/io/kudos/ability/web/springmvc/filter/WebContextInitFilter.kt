package io.kudos.ability.web.springmvc.filter

import io.kudos.ability.web.springmvc.support.getBrowserInfo
import io.kudos.ability.web.springmvc.support.getOsInfo
import io.kudos.ability.web.springmvc.support.getRemoteIp
import io.kudos.context.core.ClientInfo
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.support.Consts
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import java.util.UUID


/**
 * Web 上下文初始化过滤器默认实现。
 *
 * 在每个请求进入业务逻辑前：
 *  1. 把 session / cookie / header 全量塞进 [KudosContext]，便于业务侧通过 [KudosContextHolder] 读
 *  2. 解析 `traceKey` 请求头（缺失则生成新 UUID）
 *  3. 解析客户端 IP / 浏览器 / OS / Referer / Locale，组装 [ClientInfo]
 *  4. 请求结束的 finally 里 `KudosContextHolder.clear()`，避免线程池里的线程被复用时串台
 *
 * 实现侧重业务可读性而非性能：会把整个 session 拷一份到 context，session 大时不友好；
 * 业务方有需要可继承本类覆盖 [doFilter] 或自实现 [IWebContextInitFilter] 取代。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class WebContextInitFilter : IWebContextInitFilter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val context = KudosContext()

        // session
        val session = (request as HttpServletRequest).session
        session.attributeNames.toList().forEach { name ->
            context.addSessionAttributes(name to session.getAttribute(name))
        }

        // cookie
        request.cookies?.forEach { cookie ->
            context.addCookieAttributes(cookie.name to cookie.value)
        }

        // header
        request.headerNames.toList().forEach { name ->
            context.addHeaderAttributes(name to request.getHeader(name))
        }
        context.traceKey = request.getHeader(Consts.RequestHeader.TRACE_KEY)?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()

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

        try {
            chain.doFilter(request, response)
        } finally {
            // 请求结束后清理所有上下文，避免线程池复用线程时造成上下文污染和内存泄漏
            KudosContextHolder.clear()
        }
    }


}
