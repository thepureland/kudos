package io.kudos.ability.distributed.notify.support

import io.kudos.ability.distributed.notify.api.INotifyListener


/**
 * 创建人： Younger
 * 日期： 2022/11/14 16:31
 * 描述：
 */
object NotifyListenerItem {
    private val notifyListenerMap: MutableMap<String?, INotifyListener<*>?> = HashMap<String?, INotifyListener<*>?>()

    fun put(key: String?, listener: INotifyListener<*>?) {
        notifyListenerMap.put(key, listener)
    }

    fun get(key: String?): INotifyListener<*>? {
        return notifyListenerMap.get(key)
    }
}
