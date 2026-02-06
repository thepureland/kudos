package io.kudos.ability.distributed.notify.common.support

import io.kudos.ability.distributed.notify.common.api.INotifyListener


/**
 * 创建人： Younger
 * 日期： 2022/11/14 16:31
 * 描述：
 */
object NotifyListenerItem {

    private val notifyListenerMap = mutableMapOf<String, INotifyListener>()

    fun put(key: String, listener: INotifyListener) {
        notifyListenerMap[key] = listener
    }

    fun get(key: String): INotifyListener? {
        return notifyListenerMap[key]
    }

}
