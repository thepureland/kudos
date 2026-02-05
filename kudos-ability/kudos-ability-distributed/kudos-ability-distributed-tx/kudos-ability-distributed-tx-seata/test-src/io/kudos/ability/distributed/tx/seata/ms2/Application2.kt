package io.kudos.ability.distributed.tx.seata.ms2

import io.kudos.ability.distributed.tx.seata.data.TestTableDao
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.EnableKudos
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource

/**
 * 微服务应用2
 *
 * @author K
 * @since 1.0.0
 */
@EnableDiscoveryClient
//@ActiveProfiles("ms2") // 在SpringApplication.run(Application2.class)方式下无效!!!
@PropertySource(
    value = ["classpath:application-ms2.yml"],
    factory = YamlPropertySourceFactory::class
)
@EnableKudos
@Import(Controller2::class, Service2::class, TestTableDao::class)
open class Application2
