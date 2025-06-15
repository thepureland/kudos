package io.kudos.ability.distributed.notify.mq.ms

import io.kudos.ability.distributed.notify.mq.common.IMainClinet
import io.kudos.ability.distributed.notify.mq.main.ms.NotifyMqMainController
import io.kudos.ability.distributed.notify.mq.main.ms.NotifyMqMsService
import io.kudos.ability.distributed.notify.mq.ms.common.DataSourceNotifyListener
import io.kudos.ability.distributed.notify.mq.ms.common.MsApplicationListener
import io.kudos.ability.distributed.notify.mq.ms.common.MsConfig
import io.kudos.context.init.EnableKudos
import io.kudos.context.spring.YamlPropertySourceFactory
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource

@EnableFeignClients(basePackageClasses = [IMainClinet::class])
@PropertySource(
    value = ["classpath:application-ms.yml"
    ], factory = YamlPropertySourceFactory::class
)
@EnableKudos
@Import(
    NotifyMqMainController::class,
    NotifyMqMsService::class,
    DataSourceNotifyListener::class,
    MsApplicationListener::class,
    MsConfig::class
)
open class MsApplication
