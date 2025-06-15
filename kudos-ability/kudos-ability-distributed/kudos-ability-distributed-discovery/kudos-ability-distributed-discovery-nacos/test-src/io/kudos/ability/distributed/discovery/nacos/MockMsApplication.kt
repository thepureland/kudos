package io.kudos.ability.distributed.discovery.nacos

import io.kudos.context.spring.YamlPropertySourceFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.PropertySource


/**
 * 模拟微服务Application
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
//@ActiveProfiles("ms") // 在SpringApplication.run(ServiceApplication.class)方式下无效!!!
open class MockMsApplication