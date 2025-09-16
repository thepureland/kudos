package io.kudos.ability.cache.common.support

object CacheCleanRegister {

    private val registerMap = mutableMapOf<String, MutableList<ICacheCleanListener>>()

    @Synchronized
    fun register(cacheKey: String, cleanListener: ICacheCleanListener) {
        if (!registerMap.containsKey(cacheKey)) {
            registerMap.put(cacheKey, ArrayList())
        }
        registerMap[cacheKey]!!.add(cleanListener)
    }

    fun getCleanListener(cacheKey: String): List<ICacheCleanListener>? {
        return registerMap[cacheKey]
    }
}
