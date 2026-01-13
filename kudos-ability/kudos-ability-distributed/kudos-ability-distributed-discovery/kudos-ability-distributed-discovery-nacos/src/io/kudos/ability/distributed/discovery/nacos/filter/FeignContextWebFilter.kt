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
 * 
 * 从HTTP请求头中提取Feign调用传递的上下文信息，并设置到KudosContext中。
 * 
 * 核心功能：
 * 1. 上下文提取：从HTTP请求头中提取租户ID、子系统代码、追踪键、数据源ID、语言环境等信息
 * 2. 上下文设置：将提取的信息设置到当前线程的KudosContext中，供后续业务逻辑使用
 * 3. 扩展支持：支持通过IFeignProviderContextProcess接口扩展上下文处理逻辑
 * 
 * 处理的请求头：
 * - TENANT_ID：租户ID
 * - SUB_SYS_CODE：子系统代码
 * - TRACE_KEY：追踪键，用于分布式链路追踪
 * - DATASOURCE_ID：数据源ID，用于动态数据源切换
 * - LOCAL：语言环境，格式为"语言代码_国家代码"（如zh_CN）
 * 
 * 工作流程：
 * - 判断请求是否为Feign请求（通过FEIGN_REQUEST或NOTIFY_REQUEST请求头）
 * - 提取请求头中的上下文信息
 * - 设置到KudosContext中
 * - 调用扩展处理器进行额外的上下文处理
 * - 继续执行过滤器链
 * 
 * 注意事项：
 * - 仅处理Feign请求，普通HTTP请求会直接放行
 * - 如果ClientInfo不存在，会自动创建
 * - 语言环境字符串会被解析为Locale对象
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
