package io.kudos.ability.distributed.notify.common.support

import io.kudos.ability.distributed.notify.common.api.INotifyListener
import java.util.concurrent.ConcurrentHashMap


/**
 * [INotifyListener] bean 注册表——按 `(namespace, type)` 索引。
 *
 * 并发约定：写入主要发生在 Spring `BeanPostProcessor` 阶段（单线程），读取在 MQ 派发阶段
 * （多线程）。改用 [ConcurrentHashMap] 把约束变成显式安全——即便业务侧在 runtime 动态注册
 * listener 也不会触发 ConcurrentModificationException 或可见性问题。`getOrPut` 在 CHM 上仍
 * 不是原子的，但本注册表"写入仅在装配期"是已知约束，这里的非原子 getOrPut 不会引入新风险。
 *
 * @author Younger
 * @author K
 * @since 1.0.0
 */
object NotifyListenerItem {

    /** 当 listener 没指定 namespace 时回落的默认值。 */
    const val DEFAULT_NAMESPACE: String = "default"

    /** namespace → (key → listener) 两级索引，两级都用 [ConcurrentHashMap]，读路径完全无锁。 */
    private val notifyListenerMap = ConcurrentHashMap<String, ConcurrentHashMap<String, INotifyListener>>()

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
        notifyListenerMap.getOrPut(actualNamespace) { ConcurrentHashMap() }[key] = listener
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
