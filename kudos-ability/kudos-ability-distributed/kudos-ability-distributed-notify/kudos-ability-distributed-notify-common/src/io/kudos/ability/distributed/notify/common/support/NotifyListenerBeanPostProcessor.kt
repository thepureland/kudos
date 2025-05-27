package io.kudos.ability.distributed.notify.support

import io.kudos.ability.distributed.notify.api.INotifyListener
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.stereotype.Component

/**
 * 创建人： Younger
 * 日期： 2022/11/14 16:29
 * 描述：
 */
@Component
class NotifyListenerBeanPostProcessor : BeanPostProcessor {
    @Throws(BeansException::class)
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is INotifyListener<*>) {
            val listener = bean
            NotifyListenerItem.put(listener.notifyType(), listener)
        }
        return bean
    }
}
