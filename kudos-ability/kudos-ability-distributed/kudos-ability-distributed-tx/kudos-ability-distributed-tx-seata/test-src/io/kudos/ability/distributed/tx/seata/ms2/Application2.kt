package io.kudos.ability.distributed.tx.seata.ms2

import io.kudos.context.init.EnableKudos
import io.kudos.context.spring.YamlPropertySourceFactory
import org.mybatis.spring.annotation.MapperScan
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
@MapperScan("io.kudos.ability.distributed.tx.seata.data")
@Import(Controller2::class, Service2::class)
open class Application2
