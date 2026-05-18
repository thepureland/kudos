package io.kudos.ability.distributed.config.nacos.listener

import com.alibaba.nacos.api.config.listener.AbstractListener


/**
 * Nacos 配置变更监听器基类。
 *
 * 直接继承 nacos SDK 自带的 [AbstractListener]，**没有添加任何额外行为**——本类的存在
 * 价值是让业务代码 `import` 时不需要再触碰 nacos SDK 的具体类型，方便未来在不破坏业务
 * 调用方的前提下挂额外能力（埋点 / 重试 / 上下文透传等）。
 *
 * 业务侧典型用法：
 * ```kotlin
 * NacosConfigServiceListener(serverAddr).addListener(dataId, group, object : AbstractConfigChangeListener() {
 *     override fun receiveConfigInfo(configInfo: String?) { /* ... */ }
 * })
 * ```
 *
 * @author hanson
 * @author K
 * @since 1.0.0
 */
abstract class AbstractConfigChangeListener : AbstractListener()
