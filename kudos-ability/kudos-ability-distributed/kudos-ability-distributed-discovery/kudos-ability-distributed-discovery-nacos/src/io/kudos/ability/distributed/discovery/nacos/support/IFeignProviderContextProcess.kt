package io.kudos.ability.distributed.discovery.nacos.support

import io.kudos.context.core.KudosContext
import jakarta.servlet.http.HttpServletRequest

interface IFeignProviderContextProcess {
    /**
     * 處理feign服務端的上下文
     *
     * @param request
     * @param context
     */
    fun processContext(request: HttpServletRequest, context: KudosContext)
}
