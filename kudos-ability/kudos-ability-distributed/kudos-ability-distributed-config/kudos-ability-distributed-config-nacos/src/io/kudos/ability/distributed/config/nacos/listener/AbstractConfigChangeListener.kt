package io.kudos.ability.distributed.config.nacos.listener

import com.alibaba.nacos.api.config.listener.AbstractListener


/**
 * Base class for Nacos config-change listeners.
 *
 * Extends the Nacos SDK's own [AbstractListener] and exposes hooks before and after change
 * handling. Application code only needs to implement [onConfigChanged]; instrumentation, retry,
 * or context propagation can override [beforeConfigChanged] / [afterConfigChanged] without
 * wrapping yet another listener layer.
 *
 * Typical application usage:
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
