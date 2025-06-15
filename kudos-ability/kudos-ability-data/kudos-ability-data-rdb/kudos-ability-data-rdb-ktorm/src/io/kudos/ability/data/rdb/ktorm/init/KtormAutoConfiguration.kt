package io.kudos.ability.data.rdb.ktorm.init

import io.kudos.ability.data.rdb.jdbc.init.JdbcAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.context.annotation.ComponentScan


/**
 * ktorm自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@ComponentScan(basePackages = ["io.kudos.ability.data.rdb.ktorm"])
@AutoConfigureAfter(JdbcAutoConfiguration::class)
open class KtormAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ability-data-rdb-ktorm"

}