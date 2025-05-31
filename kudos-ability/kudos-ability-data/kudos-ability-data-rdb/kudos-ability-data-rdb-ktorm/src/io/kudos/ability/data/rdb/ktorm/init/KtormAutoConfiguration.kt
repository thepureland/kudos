package io.kudos.ability.data.rdb.ktorm.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.Configuration


/**
 * ktorm自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
open class KtormAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ability-data-rdb-ktorm"

}