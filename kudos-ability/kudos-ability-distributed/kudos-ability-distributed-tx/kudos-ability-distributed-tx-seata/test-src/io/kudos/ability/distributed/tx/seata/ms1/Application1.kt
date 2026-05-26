package io.kudos.ability.distributed.tx.seata.ms1

import io.kudos.ability.distributed.tx.seata.data.TestTableDao
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.EnableKudos
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource

/**
 * Microservice application 1.
 *
 * @author K
 * @since 1.0.0
 */
@EnableDiscoveryClient
//@ActiveProfiles("ms1") // Has no effect when launched via SpringApplication.run(Application1.class)
@PropertySource(
    value = ["classpath:application-ms1.yml"],
    factory = YamlPropertySourceFactory::class
)
@EnableKudos
@Import(Controller1::class, Service1::class, TestTableDao::class)
open class Application1
