package io.kudos.ability.distributed.client.feign.interceptor

import feign.RequestInterceptor
import feign.RequestTemplate
import io.kudos.ability.distributed.client.feign.support.IFeignRequestContextProcess
import io.kudos.context.support.Consts
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import java.util.*

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
            for (value in contextProcessMap!!.values) {
                value.processContext(requestTemplate, context)
            }
        }
    }

}
