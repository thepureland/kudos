package io.kudos.ability.data.rdb.ktorm.init

import io.kudos.ability.data.rdb.jdbc.init.JdbcAutoConfiguration
import io.kudos.base.logger.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.*
import javax.annotation.PostConstruct


/**
 * ktorm自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@ComponentScan(
    basePackages = [
        "io.kudos.ability.data.rdb.ktorm"
    ], excludeFilters = [ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, classes = [KtormAutoConfiguration::class]
    )]
)
@AutoConfigureAfter(JdbcAutoConfiguration::class)
open class KtormAutoConfiguration {

    private val logger = LoggerFactory.getLogger(this)

    @Bean("int")
    open fun int(): Pair<Int, Int> {
        logger.info("KtormAutoConfiguration::int()")
        return Pair(1, 2)
    }

    @Bean("int2")
    open fun int2(): Pair<Int, Int> {
        logger.info("KtormAutoConfiguration::int2()")
        return Pair(1, 2)
    }

    @PostConstruct
    open fun init() {
        logger.info("【kudos-ability-data-rdb-ktorm】初始化完成.")
    }

}