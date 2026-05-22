package io.kudos.ability.distributed.config.nacos.listener

import com.alibaba.nacos.api.config.listener.AbstractListener


/**
 * Nacos 配置变更监听器基类。
 *
 * 继承 nacos SDK 自带的 [AbstractListener]，并提供变更处理前后的 hook。业务侧实现
 * [onConfigChanged] 即可；需要埋点 / 重试 / 上下文透传时可覆盖 [beforeConfigChanged] /
 * [afterConfigChanged]，无需再包一层 listener。
 *
 * 业务侧典型用法：
 * ```kotlin
 * NacosConfigServiceListener(serverAddr).addListener(dataId, group, object : AbstractConfigChangeListener() {
 *     override fun onConfigChanged(configInfo: String?) { /* ... */ }
 * })
 * ```
 *
 * @author hanson
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
abstract class AbstractConfigChangeListener : AbstractListener() {

    override fun receiveConfigInfo(configInfo: String?) {
        beforeConfigChanged(configInfo)
        runCatching {
            onConfigChanged(configInfo)
        }.onSuccess {
            afterConfigChanged(configInfo, null)
        }.onFailure {
            afterConfigChanged(configInfo, it)
            throw it
        }
    }

    protected open fun beforeConfigChanged(configInfo: String?) = Unit

    protected open fun onConfigChanged(configInfo: String?) = Unit

    protected open fun afterConfigChanged(configInfo: String?, cause: Throwable?) = Unit
}
