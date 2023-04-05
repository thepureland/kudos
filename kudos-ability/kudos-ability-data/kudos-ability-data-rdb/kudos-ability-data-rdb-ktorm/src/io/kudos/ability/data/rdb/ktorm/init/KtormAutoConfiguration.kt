package io.kudos.ability.data.rdb.ktorm.init

import io.kudos.ability.data.rdb.jdbc.init.JdbcAutoConfiguration
import io.kudos.base.logger.LoggerFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.*
import javax.annotation.PostConstruct


/**
 * ktorm自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@ComponentScan(basePackages = ["io.kudos.ability.data.rdb.ktorm"])
@AutoConfigureAfter(JdbcAutoConfiguration::class)
open class KtormAutoConfiguration : IComponentInitializer {

    private val logger = LoggerFactory.getLogger(this)

    @PostConstruct
    override fun init() {
        logger.info("【kudos-ability-data-rdb-ktorm】初始化完成.")
    }

}