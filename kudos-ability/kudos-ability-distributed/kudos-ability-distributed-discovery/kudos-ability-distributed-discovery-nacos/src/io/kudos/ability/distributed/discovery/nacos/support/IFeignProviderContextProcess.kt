package io.kudos.ability.distributed.discovery.nacos.support

import io.kudos.context.core.KudosContext
import jakarta.servlet.http.HttpServletRequest

/**
 * Feign服务端上下文处理器接口
 * 用于扩展处理Feign服务端接收到的上下文信息
 */
interface IFeignProviderContextProcess {
    /**
     * 處理feign服務端的上下文
     *
     * @param request
     * @param context
     */
    fun processContext(request: HttpServletRequest, context: KudosContext)
}
