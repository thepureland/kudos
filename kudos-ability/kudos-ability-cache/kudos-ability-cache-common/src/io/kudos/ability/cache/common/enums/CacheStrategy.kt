package io.kudos.ability.cache.common.enums

/**
 * 缓存策略
 */
enum class CacheStrategy {
    SINGLE_LOCAL,  //单节点本地缓存
    REMOTE,  //远程缓存
    LOCAL_REMOTE,  //本地-远程两级联动缓存;
}
