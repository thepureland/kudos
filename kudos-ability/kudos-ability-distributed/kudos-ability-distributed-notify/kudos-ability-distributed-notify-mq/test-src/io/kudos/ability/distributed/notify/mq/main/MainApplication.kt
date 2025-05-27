package io.kudos.ability.distributed.notify.mq.main

import io.kudos.context.init.EnableKudos
import io.kudos.context.spring.YamlPropertySourceFactory
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.PropertySource

@EnableDiscoveryClient
@ComponentScan(
    basePackages = ["org.soul.ability.distributed.notify.mq.main.ms"
    ]
)
@PropertySource(
    value = ["classpath:application-main.yml"
    ], factory = YamlPropertySourceFactory::class
)
@EnableKudos
class MainApplication 
