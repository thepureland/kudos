package io.kudos.ability.distributed.notify.common.support

import io.kudos.ability.distributed.notify.common.api.INotifyListener
import org.springframework.beans.factory.config.BeanPostProcessor

/**
 * 自动注册 [INotifyListener] bean 到 [NotifyListenerItem]。
 *
 * 装配时 Spring 调每个 bean 一次 `postProcessAfterInitialization`，本类按 `notifyType()` 把
 * listener 实例登记进 namespace 下的注册表，后续 MQ 收到消息时按 type → namespace 派发。
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
