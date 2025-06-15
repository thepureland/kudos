package io.kudos.ability.distributed.notify.rdb.ms

import io.kudos.ability.distributed.notify.rdb.ms.common.NotifyRdbMsController
import io.kudos.ability.distributed.notify.rdb.ms.common.NotifyRdbMsService
import io.kudos.ability.distributed.notify.rdb.ms.common.RdbDataSourceNotifyListener
import io.kudos.context.init.EnableKudos
import io.kudos.context.spring.YamlPropertySourceFactory
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource
import org.springframework.test.context.ActiveProfiles

@EnableDiscoveryClient
@PropertySource(
    value = ["classpath:application-ms.yml"
    ], factory = YamlPropertySourceFactory::class
)
@EnableKudos
@ActiveProfiles("ms")
@Import(NotifyRdbMsController::class, NotifyRdbMsService::class, RdbDataSourceNotifyListener::class)
open class RdbMsApplication
