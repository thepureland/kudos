package io.kudos.ability.distributed.tx.seata.ms2

import org.mybatis.spring.annotation.MapperScan
import org.soul.context.context.EnableSoul
import org.soul.context.core.SoulPropertySourceFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.PropertySource

/**
 * 微服务应用2
 *
 * @author will
 * @since 5.1.1
 */
@EnableDiscoveryClient //@ActiveProfiles("ms2") // 在SpringApplication.run(Application2.class)方式下无效!!!
@PropertySource(
    value = ["classpath:application-ms2.yml"
    ], factory = SoulPropertySourceFactory::class
)
@EnableSoul
@MapperScan("org.soul.ability.distributed.tx.seata.data")
@SpringBootApplication
class Application2 
