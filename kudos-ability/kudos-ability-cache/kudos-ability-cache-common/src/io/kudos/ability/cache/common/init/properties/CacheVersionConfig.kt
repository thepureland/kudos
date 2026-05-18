package io.kudos.ability.cache.common.init.properties

import io.kudos.context.support.Consts
import org.springframework.beans.factory.annotation.Value

/**
 * 缓存版本与命名隔离配置。
 *
 * 把所有缓存项的实际 key 加上 `<version>:` 前缀，实现"灰度发布""蓝绿部署""数据格式变更"的
 * 隔离——升级后改一下 `kudos.ability.cache.version` 就可以与老版本数据并存而不互踩。
 * 同时也会用于分布式失效广播的 channel 前缀（[realMsgChannel]）。
 *
 * @author K
 * @since 1.0.0
 */
class CacheVersionConfig {

    /** 当前缓存版本；为空串时不加前缀，等价于"不启用版本隔离"。 */
    @Value($$"${kudos.ability.cache.version:default}")
    var cacheVersion: String = "default"

    /** 把逻辑缓存名转为带版本前缀的真实名（`<version>:<name>`）。版本为空则原样返回。 */
    fun getFinalCacheName(cacheName: String): String {
        if (cacheVersion.isBlank()) {
            return cacheName
        }
        return cacheVersion + Consts.CACHE_KEY_DEFAULT_DELIMITER + cacheName
    }

    /** 把带版本前缀的真实名剥回逻辑名；前缀不匹配时原样返回（兼容历史无前缀数据）。 */
    fun getRealCacheName(cacheName: String): String {
        return if (cacheName.startsWith(cacheVersion)) {
            cacheName.replace(cacheVersion + Consts.CACHE_KEY_DEFAULT_DELIMITER, "")
        } else {
            cacheName
        }
    }

    /** 分布式失效广播的真实 channel：`<version>:cache:local-remote:channel`。 */
    val realMsgChannel: String
        get() = "$cacheVersion:$MSG_CHANNEL"

    companion object {
        private const val MSG_CHANNEL = "cache:local-remote:channel"
    }

}
