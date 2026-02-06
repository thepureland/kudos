package io.kudos.ability.cache.local.caffeine.keyvalue

import io.kudos.base.lang.string.RandomStringKit
import io.kudos.base.logger.LogFactory
import io.kudos.context.kit.SpringKit
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable


/**
 * 缓存测试模拟服务
 *
 * @author K
 * @since 1.0.0
 */
open class CacheTestService {

    fun getData(id: String): String {
        return SpringKit.getBean(CacheTestService::class).getFromDB(id)
    }

    @Cacheable(cacheNames = ["test"], key = "#id")
    open fun getFromDB(id: String): String {
        LogFactory.getLog(CacheTestService::class).info("模拟去db查询~~~$id")
        return RandomStringKit.uuidWithoutDelimiter()
    }

    @Value($$"${config.info:DEFAULT_VALUE}")
    private val configRemote: String? = null

    @Value($$"${spring.cloud.config.uri:DEFAULT_VALUE}")
    private val configLocal: String? = null

    @Value($$"${configNoExists:DEFAULT_VALUE}")
    private val configNoExists: String? = null

}