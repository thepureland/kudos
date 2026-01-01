package io.kudos.ability.data.vdb.pgvector.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.Configuration


/**
 * postgres vector自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
open class PgVectorAutoConfiguration : IComponentInitializer {



    override fun getComponentName() = "kudos-ability-data-vector-pgvector"

}