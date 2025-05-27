package io.kudos.ability.distributed.notify.rdb.ms.common

import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NotifyRdbTestConfiguration : ApplicationListener<WebServerInitializedEvent?> {
    private val log: Log = LogFactory.getLog(NotifyRdbTestConfiguration::class.java)

    @Bean
    fun rdbDataSourceNotifyListener(): RdbDataSourceNotifyListener {
        return RdbDataSourceNotifyListener()
    }

    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        val port = event.getWebServer().getPort()
        rdbDataSourceNotifyListener().setPort(port)
        log.info("@@ onApplicationEvent: {0}", port)
    }
}
