package io.kudos.ability.cache.common.enums

/**
 * 缓存策略。决定单个缓存项落到哪里：仅本地 / 仅远程 / 二级联动。
 *
 * 由 `MixCacheManager` 在装配阶段读取，进而选定具体的 `Cache` 实现（Caffeine / Redis / Mix）。
 * 失效广播也以本枚举为依据：[LOCAL_REMOTE] 才会推送跨节点 invalidate 消息。
 *
 * @author K
 * @since 1.0.0
 */
enum class CacheStrategy {
    /** 单节点本地缓存（如 Caffeine）。跨节点同步需通过 MQ + `CacheNotifyListener` 自行触发。 */
    SINGLE_LOCAL,

    /** 仅远程缓存（如 Redis），无本地副本。所有节点直接读写同一份。 */
    REMOTE,

    /** 二级联动：先本地、未命中回退远程；写入广播失效消息以让其他节点剔除本地副本。 */
    LOCAL_REMOTE,
}
