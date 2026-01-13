package io.kudos.ability.distributed.client.feign.interceptor

import feign.RequestInterceptor
import feign.RequestTemplate
import io.kudos.ability.distributed.client.feign.support.IFeignRequestContextProcess
import io.kudos.context.support.Consts
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import java.util.*

/**
 * 全局Feign请求拦截器
 * 
 * 自动将KudosContext中的上下文信息（租户ID、子系统代码、追踪键等）添加到Feign请求头中。
 * 
 * 核心功能：
 * 1. 上下文传递：将当前线程的KudosContext信息传递到Feign请求头，实现跨服务上下文传递
 * 2. 追踪键生成：如果上下文中没有追踪键，会自动生成UUID作为追踪键
 * 3. 扩展支持：支持通过IFeignRequestContextProcess接口扩展请求头处理逻辑
 * 
 * 添加的请求头：
 * - TENANT_ID：租户ID
 * - SUB_SYS_CODE：子系统代码
 * - TRACE_KEY：追踪键，用于分布式链路追踪
 * - DATASOURCE_ID：数据源ID（如果存在）
 * - LOCAL：语言环境，默认zh_CN
 * - FEIGN_REQUEST：标识为Feign请求，值为"true"
 * 
 * 工作流程：
 * - 从KudosContextHolder获取当前上下文
 * - 提取上下文中的各种信息
 * - 如果追踪键为空，生成UUID作为追踪键
 * - 将所有信息添加到请求头
 * - 调用扩展处理器进行额外的请求头处理
 * 
 * 注意事项：
 * - 该拦截器会应用到所有Feign请求
 * - 追踪键如果为空会自动生成，确保每次请求都有追踪标识
 * - 语言环境如果不存在，默认使用zh_CN
 */
class GlobalHeaderRequestInterceptor : RequestInterceptor {

    override fun apply(requestTemplate: RequestTemplate) {
        //从当前上下文中获取tenantId和subSysCode
        val context = KudosContextHolder.get()
        val tenantId = context.tenantId
        val subSysCode = context.subSystemCode
        var traceKey = context.traceKey
        if (traceKey.isNullOrBlank()) {
            traceKey = UUID.randomUUID().toString()
        }
        val dataSourceId = context.dataSourceId
        requestTemplate.header(Consts.RequestHeader.TENANT_ID, tenantId.toString())
        requestTemplate.header(Consts.RequestHeader.SUB_SYS_CODE, subSysCode)
        requestTemplate.header(Consts.RequestHeader.TRACE_KEY, traceKey)
        val locale = context.clientInfo?.locale
        if (locale != null) {
            requestTemplate.header(Consts.RequestHeader.LOCAL, locale.toString())
        } else {
            requestTemplate.header(Consts.RequestHeader.LOCAL, "zh_CN")
        }
        if (dataSourceId != null) {
            requestTemplate.header(Consts.RequestHeader.DATASOURCE_ID, dataSourceId)
        }
        requestTemplate.header(Consts.RequestHeader.FEIGN_REQUEST, "true")
        val contextProcessMap = SpringKit.getBeansOfType(IFeignRequestContextProcess::class)
        if (contextProcessMap.isNotEmpty()) {
            for (value in contextProcessMap.values) {
                value.processContext(requestTemplate, context)
            }
        }
    }

}
