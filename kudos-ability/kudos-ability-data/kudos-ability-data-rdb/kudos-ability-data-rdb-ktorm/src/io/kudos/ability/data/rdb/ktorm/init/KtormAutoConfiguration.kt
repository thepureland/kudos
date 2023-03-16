package io.kudos.ability.data.rdb.ktorm.init

import io.dudos.ability.data.rdb.jdbc.init.EnableJdbc
import io.kudos.base.logger.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.*
import javax.annotation.PostConstruct


/**
 * ktorm自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@EnableJdbc
@ComponentScan(
    basePackages = [
        "io.dudos.ability.data.rdb.ktorm"
    ]
)
@AutoConfigureOrder(3000)
//@AutoConfigureAfter(JdbcAutoConfiguration::class)
open class KtormAutoConfiguration {

    private val logger = LoggerFactory.getLogger(this)

    @PostConstruct
    open fun init() {
        logger.info("【kudos-ability-data-rdb-ktorm】初始化完成.")
    }

}