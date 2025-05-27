package io.kudos.ability.distributed.notify.rdb.ms

import org.soul.context.context.EnableSoul
import org.soul.context.core.SoulPropertySourceFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ActiveProfiles

@EnableDiscoveryClient
@ComponentScan(
    basePackages = [ //                "org.soul.ability.distributed.notify.test.rdb.common",
        "org.soul.ability.distributed.notify.test.rdb.ms"
    ]
)
@PropertySource(
    value = ["classpath:application-ms.yml"
    ], factory = SoulPropertySourceFactory::class
)
@EnableSoul
@ActiveProfiles("ms")
@SpringBootApplication
class RdbMsApplication 
