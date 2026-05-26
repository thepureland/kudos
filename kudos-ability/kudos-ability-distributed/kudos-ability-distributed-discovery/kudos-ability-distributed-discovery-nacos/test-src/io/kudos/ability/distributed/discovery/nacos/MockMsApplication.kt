package io.kudos.ability.distributed.discovery.nacos

import io.kudos.context.config.YamlPropertySourceFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.PropertySource


/**
 * Mock microservice Application.
 *
 * @author K
 * @since 1.0.0
 */
@EnableDiscoveryClient
@SpringBootApplication
@PropertySource(
    value = ["classpath:application-ms.yml"
    ], factory = YamlPropertySourceFactory::class
)
//@ActiveProfiles("ms") // Has no effect when launching via SpringApplication.run(ServiceApplication.class)!!!
open class MockMsApplication