package io.kudos.ability.cache.interservice.provider.init

/**
 * 跨服务缓存 provider 端配置。
 *
 * @property uidCacheEnabled 是否缓存响应对象到 UID 的映射。默认关闭，避免可变对象导致旧 UID 复用。
 * @property wrapAllRequests 是否让 filter 包装所有请求。默认只包装带跨服务缓存请求头的请求。
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class InterServiceCacheProviderProperties {
    var uidCacheEnabled: Boolean = false
    var wrapAllRequests: Boolean = false
}
