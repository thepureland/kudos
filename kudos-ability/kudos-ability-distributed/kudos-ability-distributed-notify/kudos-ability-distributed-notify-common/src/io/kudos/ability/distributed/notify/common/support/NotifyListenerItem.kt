package io.kudos.ability.distributed.notify.common.support

import io.kudos.ability.distributed.notify.common.api.INotifyListener


/**
 * 创建人： Younger
 * 日期： 2022/11/14 16:31
 * 描述：
 */
object NotifyListenerItem {

    const val DEFAULT_NAMESPACE: String = "default"

    private val notifyListenerMap = mutableMapOf<String, MutableMap<String, INotifyListener>>()

    fun put(namespace: String, key: String, listener: INotifyListener) {
        val actualNamespace = namespace.ifBlank { DEFAULT_NAMESPACE }
        notifyListenerMap.getOrPut(actualNamespace) { mutableMapOf() }[key] = listener
    }

    fun put(key: String, listener: INotifyListener) {
        put(DEFAULT_NAMESPACE, key, listener)
    }

    fun get(namespace: String, key: String): INotifyListener? {
        val actualNamespace = namespace.ifBlank { DEFAULT_NAMESPACE }
        return notifyListenerMap[actualNamespace]?.get(key)
    }

    fun get(key: String): INotifyListener? = get(DEFAULT_NAMESPACE, key)

}
