package io.kudos.ability.distributed.tx.seata.ms1

import org.mybatis.spring.annotation.MapperScan
import org.soul.context.context.EnableSoul
import org.soul.context.core.SoulPropertySourceFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.PropertySource

/**
 * 微服务应用1
 *
 * @author will
 * @since 5.1.1
 */
@EnableDiscoveryClient //@ActiveProfiles("ms1") // 在SpringApplication.run(Application1.class)方式下无效!!!
@PropertySource(
    value = ["classpath:application-ms1.yml"
    ], factory = SoulPropertySourceFactory::class
)
@EnableSoul
@MapperScan("org.soul.ability.distributed.tx.seata.data")
@SpringBootApplication
class Application1 
