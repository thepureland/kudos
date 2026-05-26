package io.kudos.ability.distributed.stream.common.support

import io.kudos.ability.distributed.stream.common.handler.IStreamFailHandler
import io.kudos.ability.distributed.stream.common.handler.StreamFailHandlerItem
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanPostProcessor

/**
 * Bean post-processor for Stream producer failure handlers.
 * Automatically registers all IStreamFailHandler implementations into the
 * StreamFailHandlerItem registry.
 */
class StreamProducerFailHandlerProcessor : BeanPostProcessor {

    @Throws(BeansException::class)
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is IStreamFailHandler) {
            val handler: IStreamFailHandler = bean
            StreamFailHandlerItem.put(
                requireNotNull(handler.bindName()) { "IStreamFailHandler.bindName() must not be null: ${handler.javaClass.name}" },
                handler
            )
        }
        return bean
    }
}
