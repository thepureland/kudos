package io.kudos.ability.distributed.notify.common.support

import io.kudos.ability.distributed.notify.common.api.INotifyListener
import org.springframework.beans.factory.config.BeanPostProcessor

/**
 * 创建人： Younger
 * 日期： 2022/11/14 16:29
 * 描述：
 */
open class NotifyListenerBeanPostProcessor : BeanPostProcessor {

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is INotifyListener) {
            NotifyListenerItem.put(bean.notifyType(), bean)
        }
        return bean
    }

}
