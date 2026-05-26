package io.kudos.ability.distributed.tx.seata.ms2

import io.kudos.ability.distributed.tx.seata.data.TestTableDao
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.EnableKudos
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource

/**
 * Microservice application 2.
 *
 * @author K
 * @since 1.0.0
 */
@EnableDiscoveryClient
//@ActiveProfiles("ms2") // Has no effect when launched via SpringApplication.run(Application2.class)
@PropertySource(
    value = ["classpath:application-ms2.yml"],
    factory = YamlPropertySourceFactory::class
)
@EnableKudos
@Import(Controller2::class, Service2::class, TestTableDao::class)
open class Application2
