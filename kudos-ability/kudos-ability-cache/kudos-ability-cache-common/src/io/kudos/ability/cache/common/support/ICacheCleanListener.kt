package io.kudos.ability.cache.common.support

import org.springframework.beans.factory.InitializingBean

interface ICacheCleanListener : InitializingBean {
    fun cleanCache(cacheName: String, key: Any?)
}
