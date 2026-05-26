package io.kudos.ability.distributed.notify.common.support

import io.kudos.ability.distributed.notify.common.api.INotifyListener
import org.springframework.beans.factory.config.BeanPostProcessor

/**
 * Automatically registers [INotifyListener] beans into [NotifyListenerItem].
 *
 * During wiring Spring calls `postProcessAfterInitialization` once per bean; this class registers each
 * listener instance into the per-namespace registry keyed by `notifyType()`, so subsequent MQ messages can
 * be dispatched by type within their namespace.
 *
 * @author Younger
 * @author K
 * @since 1.0.0
 */
open class NotifyListenerBeanPostProcessor(
    private val namespace: String = NotifyListenerItem.DEFAULT_NAMESPACE
) : BeanPostProcessor {

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is INotifyListener) {
            NotifyListenerItem.put(namespace, bean.notifyType(), bean)
        }
        return bean
    }

}
