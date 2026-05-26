package io.kudos.ability.distributed.client.feign.ms

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
//@ActiveProfiles("ms") // Has no effect when launched via SpringApplication.run(ServiceApplication.class)!!!
@EnableDiscoveryClient
@SpringBootApplication
@PropertySource(
    value = ["classpath:application-ms.yml"
    ], factory = YamlPropertySourceFactory::class
)
open class MockMsApplication
