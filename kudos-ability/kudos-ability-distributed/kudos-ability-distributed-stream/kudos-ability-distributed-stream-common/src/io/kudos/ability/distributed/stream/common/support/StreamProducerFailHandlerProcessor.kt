package io.kudos.ability.distributed.stream.common.support

import io.kudos.ability.distributed.stream.common.handler.IStreamFailHandler
import io.kudos.ability.distributed.stream.common.handler.StreamFailHandlerItem
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanPostProcessor

/**
 * 流式消息生产者失败处理器后置处理器
 * 自动注册所有IStreamFailHandler实现类到StreamFailHandlerItem注册表中
 */
class StreamProducerFailHandlerProcessor : BeanPostProcessor {
    
    @Throws(BeansException::class)
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is IStreamFailHandler) {
            val handler: IStreamFailHandler = bean
            StreamFailHandlerItem.put(handler.bindName()!!, handler)
        }
        return bean
    }
}
