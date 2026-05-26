package io.kudos.ability.cache.interservice.provider

import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.EnableKudos
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource


/**
 * Mock microservice Application.
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudos
@PropertySource(
    value = ["classpath:application-ms.yml"
    ], factory = YamlPropertySourceFactory::class
)
//@ActiveProfiles("ms") // Ineffective under SpringApplication.run(ServiceApplication.class)!!!
@Import(MockMsController::class)
open class MockMsApplication