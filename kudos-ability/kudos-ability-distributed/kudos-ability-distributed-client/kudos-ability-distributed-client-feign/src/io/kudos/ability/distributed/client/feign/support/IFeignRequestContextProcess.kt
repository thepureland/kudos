package io.kudos.ability.distributed.client.feign.support

import feign.RequestTemplate
import io.kudos.context.core.KudosContext

/**
 * feignContext擴展處理
 */
interface IFeignRequestContextProcess {
    /**
     * feign處理器擴展
     *
     * @param requestTemplate
     * @param contextParam
     */
    fun processContext(requestTemplate: RequestTemplate, context: KudosContext)
}
