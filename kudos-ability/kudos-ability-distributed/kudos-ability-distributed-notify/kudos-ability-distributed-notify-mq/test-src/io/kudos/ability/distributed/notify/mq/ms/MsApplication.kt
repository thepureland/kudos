package io.kudos.ability.distributed.notify.mq.ms

import io.kudos.ability.distributed.notify.mq.common.IMainClinet
import io.kudos.context.init.EnableKudos
import io.kudos.context.spring.YamlPropertySourceFactory
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.PropertySource

@EnableFeignClients(basePackageClasses = [IMainClinet::class])
@ComponentScan(
    basePackages = ["org.soul.ability.distributed.notify.mq.common", "org.soul.ability.distributed.notify.mq.ms.common"
    ]
)
@PropertySource(
    value = ["classpath:application-ms.yml"
    ], factory = YamlPropertySourceFactory::class
)
@EnableKudos
class MsApplication 
