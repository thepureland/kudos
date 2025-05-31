package io.kudos.ability.distributed.notify.rdb.ms.common

import io.kudos.base.logger.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class NotifyRdbTestConfiguration : ApplicationListener<WebServerInitializedEvent> {

    private val log = LoggerFactory.getLogger(this)

    @Autowired
    private lateinit var rdbDataSourceNotifyListener: RdbDataSourceNotifyListener

    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        val port = event.webServer.port
        rdbDataSourceNotifyListener.setPort(port)
        log.info("@@ onApplicationEvent: $port")
    }

}
