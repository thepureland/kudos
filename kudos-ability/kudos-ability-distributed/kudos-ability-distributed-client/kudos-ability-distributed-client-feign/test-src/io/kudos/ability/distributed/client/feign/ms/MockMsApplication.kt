package io.kudos.ability.distributed.client.feign.ms

import io.kudos.context.config.YamlPropertySourceFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.PropertySource

/**
 * 模拟微服务Application
 *
 * @author K
 * @since 1.0.0
 */
//@ActiveProfiles("ms") // 在SpringApplication.run(ServiceApplication.class)方式下无效!!!
@EnableDiscoveryClient
@SpringBootApplication
@PropertySource(
    value = ["classpath:application-ms.yml"
    ], factory = YamlPropertySourceFactory::class
)
open class MockMsApplication
