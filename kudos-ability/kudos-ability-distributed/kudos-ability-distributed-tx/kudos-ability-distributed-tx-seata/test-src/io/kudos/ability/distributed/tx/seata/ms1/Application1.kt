package io.kudos.ability.distributed.tx.seata.ms1

import io.kudos.ability.distributed.tx.seata.data.TestTableDao
import io.kudos.context.init.EnableKudos
import io.kudos.context.config.YamlPropertySourceFactory
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource

/**
 * 微服务应用1
 *
 * @author K
 * @since 1.0.0
 */
@EnableDiscoveryClient
//@ActiveProfiles("ms1") // 在SpringApplication.run(Application1.class)方式下无效!!!
@PropertySource(
    value = ["classpath:application-ms1.yml"],
    factory = YamlPropertySourceFactory::class
)
@EnableKudos
@Import(Controller1::class, Service1::class, TestTableDao::class)
open class Application1
