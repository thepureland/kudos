package io.kudos.ability.cache.remote.redis

import io.kudos.base.lang.string.RandomStringKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable

open class CacheTestService {

//    @Autowired
//    private lateinit var self: CacheTestService

    fun getData(id: String): String {
        return SpringKit.getBean(CacheTestService::class).getFromDB(id)
    }

    @Cacheable(cacheNames = ["test"], key = "#id")
    open fun getFromDB(id: String): String {
        LogFactory.getLog(CacheTestService::class).info("模拟去db查询~~~$id")
        return RandomStringKit.uuidWithoutDelimiter()
    }

    @Value("\${config.info:DEFAULT_VALUE}")
    private val configRemote: String? = null

    @Value("\${spring.cloud.config.uri:DEFAULT_VALUE}")
    private val configLocal: String? = null

    @Value("\${configNoExists:DEFAULT_VALUE}")
    private val configNoExists: String? = null

//    /**
//     * 从配置中心获取属性，需要启动注册中心和配置中心
//     */
//    @Test
//    fun getConfigRemote() {
//        println("configRemote:  $configRemote")
//    }
//
//    /**
//     * 从本地配置文件获取属性，不用启动注册中心和配置中心
//     */
//    @Test
//    fun getConfigLocal() {
//        println("configLocal:  $configLocal")
//    }
//
//    /**
//     * 不存在的配置，取默认值
//     */
//    @Test
//    fun getConfigNoExists() {
//        println("configNoExists:  $configNoExists")
//    }

}