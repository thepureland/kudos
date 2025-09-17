package io.kudos.ability.cache.interservice.provider

import io.kudos.context.init.EnableKudos
import io.kudos.context.config.YamlPropertySourceFactory
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource


/**
 * 模拟微服务Application
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudos
@PropertySource(
    value = ["classpath:application-ms.yml"
    ], factory = YamlPropertySourceFactory::class
)
//@ActiveProfiles("ms") // 在SpringApplication.run(ServiceApplication.class)方式下无效!!!
@Import(MockMsController::class)
open class MockMsApplication