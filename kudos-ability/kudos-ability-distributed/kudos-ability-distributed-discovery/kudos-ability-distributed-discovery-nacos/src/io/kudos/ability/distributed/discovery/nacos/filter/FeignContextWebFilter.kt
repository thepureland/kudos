package io.kudos.ability.distributed.discovery.nacos.filter

import io.kudos.ability.distributed.discovery.nacos.support.IFeignProviderContextProcess
import io.kudos.context.core.ClientInfo
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.support.Consts
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import java.util.*

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
 * - 判断请求是否带 Feign / 通知透传标记（[Consts.RequestHeader.FEIGN_REQUEST] 或 [Consts.RequestHeader.NOTIFY_REQUEST] 非空）
 * - 提取请求头中的上下文信息
 * - 设置到KudosContext中
 * - 调用扩展处理器进行额外的上下文处理
 * - 继续执行过滤器链
 * 
 * 注意事项：
 * - 无上述标记的普通 HTTP 请求直接放行
 * - 如果ClientInfo不存在，会自动创建
 * - 语言环境字符串会被解析为Locale对象
 */
class FeignContextWebFilter : Filter {

    private val log = LogFactory.getLog(this::class)

    /**
     * 执行过滤：提取Feign请求的上下文信息
     * 
     * 从HTTP请求头中提取Feign调用传递的上下文信息，并设置到KudosContext中。
     * 
     * 请求判断逻辑：
     * - 仅当 FEIGN_REQUEST 或 NOTIFY_REQUEST 任一请求头非空时解析上下文（二者为显式透传标记）
     * 
     * 上下文提取和设置：
     * 1. 从请求头提取：租户ID、子系统代码、追踪键、数据源ID、语言环境
     * 2. 获取或创建ClientInfo对象
     * 3. 解析语言环境字符串（格式：语言代码_国家代码，如zh_CN）
     * 4. 设置到KudosContext中
     * 
     * 扩展处理：
     * - 调用所有IFeignProviderContextProcess实现类进行额外的上下文处理
     * - 支持自定义扩展，例如添加额外的上下文信息
     * 
     * 注意事项：
     * - 无透传标记的请求不修改上下文
     * - 如果ClientInfo不存在，会自动创建
     * - 语言环境字符串会被解析为Locale对象
     * - 数据源ID如果为空，不会设置到上下文中
     * 
     * @param servletRequest HTTP请求对象
     * @param servletResponse HTTP响应对象
     * @param filterChain 过滤器链
     */
    override fun doFilter(
        servletRequest: ServletRequest?,
        servletResponse: ServletResponse?,
        filterChain: FilterChain
    ) {
        val request: HttpServletRequest = servletRequest as HttpServletRequest
        val isFeign = !request.getHeader(Consts.RequestHeader.FEIGN_REQUEST).isNullOrBlank()
        val isNotify = !request.getHeader(Consts.RequestHeader.NOTIFY_REQUEST).isNullOrBlank()
        if (!isFeign && !isNotify) {
            filterChain.doFilter(servletRequest, servletResponse)
            return
        }

        val tenantId: String? = request.getHeader(Consts.RequestHeader.TENANT_ID)
        val subSysCode: String? = request.getHeader(Consts.RequestHeader.SUB_SYS_CODE)
        val opKey: String? = request.getHeader(Consts.RequestHeader.TRACE_KEY)
        val dataSourceId: String? = request.getHeader(Consts.RequestHeader.DATASOURCE_ID)
        val local: String? = request.getHeader(Consts.RequestHeader.LOCAL)
        val context = KudosContextHolder.get()
        var clientInfo = context.clientInfo
        if (clientInfo == null) {
            clientInfo = ClientInfo(ClientInfo.Builder())
            context.clientInfo = clientInfo
        }
        if (!local.isNullOrBlank()) {
            val parts = local.split("_").filter { it.isNotEmpty() }
            if (parts.size >= 2) {
                clientInfo.locale = Locale.of(parts[0], parts[1])
            }
        }
        context.tenantId = tenantId
        context.subSystemCode = subSysCode
        context.traceKey = opKey
        if (!dataSourceId.isNullOrBlank()) {
            context.dataSourceId = dataSourceId
        }
        val contextProcessMap = SpringKit.getBeansOfType<IFeignProviderContextProcess>()
        if (contextProcessMap.isNotEmpty()) {
            for (value in contextProcessMap.values) {
                try {
                    value.processContext(request, context)
                } catch (e: Exception) {
                    log.error(e, "IFeignProviderContextProcess 执行失败: {0}", value.javaClass.name)
                }
            }
        }
        filterChain.doFilter(servletRequest, servletResponse)
    }
}
