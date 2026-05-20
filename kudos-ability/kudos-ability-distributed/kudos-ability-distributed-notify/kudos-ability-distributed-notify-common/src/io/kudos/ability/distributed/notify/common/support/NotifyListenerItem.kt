package io.kudos.ability.distributed.notify.common.support

import io.kudos.ability.distributed.notify.common.api.INotifyListener


/**
 * [INotifyListener] bean 注册表——按 `(namespace, type)` 索引。
 *
 * 并发约定：写入仅在 Spring `BeanPostProcessor` 阶段（单线程）；读取在 MQ 派发阶段
 * （多线程）。`mutableMapOf` 没有显式同步——依赖 Spring 装配完成后才有 MQ 流量到达
 * 的隐式 happens-before。如果业务侧在 runtime 动态注册 listener，需要自行 sync。
 *
 * @author Younger
 * @author K
 * @since 1.0.0
 */
object NotifyListenerItem {

    /** 当 listener 没指定 namespace 时回落的默认值。 */
    const val DEFAULT_NAMESPACE: String = "default"

    /** namespace → (key → listener) 两级索引；不显式同步，依赖 Spring 装配完毕的隐式 happens-before */
    private val notifyListenerMap = mutableMapOf<String, MutableMap<String, INotifyListener>>()

    /**
     * 在指定 namespace 下注册 listener；空白 namespace 自动回落 [DEFAULT_NAMESPACE]。
     *
     * @param namespace 命名空间（业务上一般对应租户 / 子系统）
     * @param key listener 业务 key
     * @param listener listener 实例
     * @author K
     * @since 1.0.0
     */
    fun put(namespace: String, key: String, listener: INotifyListener) {
        val actualNamespace = namespace.ifBlank { DEFAULT_NAMESPACE }
        notifyListenerMap.getOrPut(actualNamespace) { mutableMapOf() }[key] = listener
    }

    /**
     * 在 [DEFAULT_NAMESPACE] 下注册 listener 的快捷重载。
     *
     * @param key listener 业务 key
     * @param listener listener 实例
     * @author K
     * @since 1.0.0
     */
    fun put(key: String, listener: INotifyListener) {
        put(DEFAULT_NAMESPACE, key, listener)
    }

    /**
     * 按 namespace + key 取出 listener；命中 namespace 但 key 不存在时返回 null。
     *
     * @param namespace 命名空间，空白回落 [DEFAULT_NAMESPACE]
     * @param key listener 业务 key
     * @return listener 实例，未注册时返回 null
     * @author K
     * @since 1.0.0
     */
    fun get(namespace: String, key: String): INotifyListener? {
        val actualNamespace = namespace.ifBlank { DEFAULT_NAMESPACE }
        return notifyListenerMap[actualNamespace]?.get(key)
    }

    /**
     * 在 [DEFAULT_NAMESPACE] 下查找 listener 的快捷重载。
     *
     * @param key listener 业务 key
     * @return listener 实例，未注册时返回 null
     * @author K
     * @since 1.0.0
     */
    fun get(key: String): INotifyListener? = get(DEFAULT_NAMESPACE, key)

}
