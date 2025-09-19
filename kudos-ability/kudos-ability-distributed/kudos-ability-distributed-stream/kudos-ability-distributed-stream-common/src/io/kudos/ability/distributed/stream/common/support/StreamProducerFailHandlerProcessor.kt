package io.kudos.ability.distributed.stream.common.support

import io.kudos.ability.distributed.stream.common.handler.IStreamFailHandler
import io.kudos.ability.distributed.stream.common.handler.StreamFailHandlerItem
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanPostProcessor


class StreamProducerFailHandlerProcessor : BeanPostProcessor {
    
    @Throws(BeansException::class)
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is IStreamFailHandler) {
            val handler: IStreamFailHandler = bean as IStreamFailHandler
            StreamFailHandlerItem.put(handler.bindName()!!, handler)
        }
        return bean
    }
}
