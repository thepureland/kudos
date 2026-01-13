package io.kudos.ability.distributed.discovery.nacos.filter

import io.kudos.ability.distributed.discovery.nacos.support.IFeignProviderContextProcess
import io.kudos.context.support.Consts
import io.kudos.context.core.ClientInfo
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import java.util.Locale

/**
 * Feign上下文Web过滤器
 * 从HTTP请求头中提取Feign调用传递的上下文信息，并设置到KudosContext中
 */
class FeignContextWebFilter : Filter {

    override fun doFilter(
        servletRequest: ServletRequest?,
        servletResponse: ServletResponse?,
        filterChain: FilterChain
    ) {
        val request: HttpServletRequest = servletRequest as HttpServletRequest
        if (!request.getHeader(Consts.RequestHeader.FEIGN_REQUEST).isNullOrBlank() ||
            request.getHeader(Consts.RequestHeader.NOTIFY_REQUEST).isNullOrBlank()
        ) {
            val tenantId: String? = request.getHeader(Consts.RequestHeader.TENANT_ID)
            val subSysCode: String? = request.getHeader(Consts.RequestHeader.SUB_SYS_CODE)
            val opKey: String? = request.getHeader(Consts.RequestHeader.TRACE_KEY)
            val dataSourceId: String = request.getHeader(Consts.RequestHeader.DATASOURCE_ID)
            val local: String = request.getHeader(Consts.RequestHeader.LOCAL)
            val context = KudosContextHolder.get()
            var clientInfo = context.clientInfo
            if (clientInfo == null) {
                clientInfo = ClientInfo(ClientInfo.Builder())
                context.clientInfo = clientInfo
            }
            if (local.isNotBlank()) {
                val arr = local.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                clientInfo.locale = Locale.of(arr[0], arr[1])
            }
            context.tenantId = tenantId
            context.subSystemCode = subSysCode
            context.traceKey = opKey
            if (dataSourceId.isNotBlank()) {
                context.dataSourceId = dataSourceId
            }
            //feign服務端上下文解析支持擴展
            val contextProcessMap = SpringKit.getBeansOfType(IFeignProviderContextProcess::class)
            if (contextProcessMap.isNotEmpty()) {
                for (value in contextProcessMap.values) {
                    value.processContext(request, context)
                }
            }
            filterChain.doFilter(servletRequest, servletResponse)
        } else {
            filterChain.doFilter(servletRequest, servletResponse)
        }
    }
}
