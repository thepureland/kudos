package io.kudos.ability.distributed.notify.mq.main

import io.kudos.ability.distributed.notify.mq.main.ms.NotifyMqMainController
import io.kudos.ability.distributed.notify.mq.main.ms.NotifyMqMsService
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.EnableKudos
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource

@EnableDiscoveryClient
@PropertySource(
    value = ["classpath:application-main.yml"
    ], factory = YamlPropertySourceFactory::class
)
@EnableKudos
@Import(NotifyMqMsService::class, NotifyMqMainController::class)
open class MainApplication
