package io.kudos.ability.data.vectordb.milvus.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.context.annotation.Configuration


/**
 * milvus自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
open class MilvusAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ability-data-vector-milvus"

}