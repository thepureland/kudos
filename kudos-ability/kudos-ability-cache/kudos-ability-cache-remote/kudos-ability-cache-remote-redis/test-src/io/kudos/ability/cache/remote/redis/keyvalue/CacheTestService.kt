package io.kudos.ability.cache.remote.redis.keyvalue

import io.kudos.base.lang.string.RandomStringKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable

/**
 * Remote K-V cache test service.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class CacheTestService {

//    @Autowired
//    private lateinit var self: CacheTestService

    fun getData(id: String): String {
        return SpringKit.getBean<CacheTestService>().getFromDB(id)
    }

    @Cacheable(cacheNames = ["test"], key = "#id")
    open fun getFromDB(id: String): String {
        LogFactory.getLog(CacheTestService::class).info("Simulating a DB query ~~~$id")
        return RandomStringKit.uuidWithoutDelimiter()
    }

    @Value($$"${config.info:DEFAULT_VALUE}")
    private val configRemote: String? = null

    @Value($$"${spring.cloud.config.uri:DEFAULT_VALUE}")
    private val configLocal: String? = null

    @Value($$"${configNoExists:DEFAULT_VALUE}")
    private val configNoExists: String? = null

//    /**
//     * Reads a property from the config center; requires the registry and config center to be running.
//     */
//    @Test
//    fun getConfigRemote() {
//        println("configRemote:  $configRemote")
//    }
//
//    /**
//     * Reads a property from the local config file; no registry or config center required.
//     */
//    @Test
//    fun getConfigLocal() {
//        println("configLocal:  $configLocal")
//    }
//
//    /**
//     * Missing property falls back to the default value.
//     */
//    @Test
//    fun getConfigNoExists() {
//        println("configNoExists:  $configNoExists")
//    }

}
