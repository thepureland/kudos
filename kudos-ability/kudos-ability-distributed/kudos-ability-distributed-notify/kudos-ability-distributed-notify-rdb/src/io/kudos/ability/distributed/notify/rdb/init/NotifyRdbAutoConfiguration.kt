package io.kudos.ability.distributed.notify.rdb.init

import io.kudos.ability.distributed.notify.common.api.INotifyProducer
import io.kudos.ability.distributed.notify.common.support.NotifyListenerBeanPostProcessor
import io.kudos.ability.distributed.notify.rdb.producer.NotifyRdbProducer
import io.kudos.ability.distributed.notify.rdb.support.AppNotifyServlet
import io.kudos.base.logger.LogFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.mybatis.spring.annotation.MapperScan
import org.soul.ability.distributed.notify.rdb.entity.SysApp
import org.soul.ability.distributed.notify.rdb.service.ISysAppService
import org.soul.ability.distributed.notify.rdb.service.SysAppService
import org.soul.ability.distributed.notify.rdb.support.AppCheckServlet
import org.soul.ability.distributed.notify.rdb.support.TaskProperties
import org.soul.base.lang.string.StringTool
import org.soul.context.core.SoulPropertySourceFactory
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.lang.management.ManagementFactory
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor
import javax.management.MalformedObjectNameException
import javax.management.ObjectName
import javax.management.Query

/**
 * 基于关系型数据库的通知的自动配置类
 *
 * @author Fei
 * @author K
 * @date 2022/12/20 11:58
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
@EnableScheduling
@EnableAsync
@PropertySource(
    value = ["classpath:soul-ability-distributed-notify-rdb.yml"],
    factory = SoulPropertySourceFactory::class
)
@MapperScan(value = ["org.soul.ability.distributed.notify.rdb.mapper"])
@Import(NotifyListenerBeanPostProcessor::class)
open class NotifyRdbAutoConfiguration
    : ApplicationListener<WebServerInitializedEvent>, AsyncConfigurer, IComponentInitializer {

    private val logger = LogFactory.getLog(this)

    @Bean
    @ConditionalOnMissingBean
    open fun sysAppService() : ISysAppService = SysAppService()

    @Bean
    @ConditionalOnMissingBean
    open fun notifyRdbProducer() : INotifyProducer = NotifyRdbProducer()

    @Bean
    @ConditionalOnMissingBean
    open fun taskProperties() = TaskProperties()

    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        logger.info("开始注册应用...")
        val app = SysApp()
        app.setAppName(event.getApplicationContext().getApplicationName())
        if (StringTool.isBlank(app.getAppName())) {
            val environment = event.getApplicationContext().getBean<Environment>(Environment::class.java)
            app.setAppName(environment.getProperty("server.servlet.context-path"))
        }
        val protocol = "HTTP/1.1"
        app.setProtocol(protocol)
        app.setIp(this.serverIp)
        app.setPort(getServerPort(protocol, event))
        val result = sysAppService().addApp(app)
        if (result) {
            logger.info("应用注册成功。")
        }
    }

    private val serverIp: String?
        get() {
            val address: InetAddress
            try {
                address = InetAddress.getLocalHost()
            } catch (e: UnknownHostException) {
                throw RuntimeException(e)
            }
            return address.getHostAddress()
        }

    private fun getServerPort(protocol: String?, event: WebServerInitializedEvent): Int {
        val serverPort = event.getWebServer().getPort()
        if (serverPort > 0) {
            return serverPort
        }

        val name: ObjectName?
        try {
            name = ObjectName("*:type=Connector,*")
        } catch (e: MalformedObjectNameException) {
            throw RuntimeException(e)
        }

        val server = ManagementFactory.getPlatformMBeanServer()
        val proExp = Query.match(Query.attr("protocol"), Query.value(protocol))
        val names = server.queryNames(name, proExp)
        for (n in names) {
            val catalina = n.domain
            if ("Catalina" == catalina) {
                return n.getKeyProperty("port").toInt()
            }
        }
        return 0
    }

    @Bean
    open fun appCheckServlet(): ServletRegistrationBean<AppCheckServlet?> {
        val bean = ServletRegistrationBean(AppCheckServlet(), "/errors/app-check.html")
        bean.setLoadOnStartup(1)
        return bean
    }

    @Bean
    open fun appNotifyServlet(): ServletRegistrationBean<AppNotifyServlet?> {
        val bean = ServletRegistrationBean(AppNotifyServlet(), "/errors/app-notify.html")
        bean.setLoadOnStartup(1)
        return bean
    }

    @Bean("appNotifyExecutor")
    override fun getAsyncExecutor(): Executor {
        val taskExecutor = ThreadPoolTaskExecutor()
        val taskProperties = taskProperties()
        // 配置核心线程池数量
        taskExecutor.corePoolSize = taskProperties.corePoolSize
        // 配置最大线程池数量
        taskExecutor.maxPoolSize = taskProperties.maxPoolSize
        /** 线程池所使用的缓冲队列 */
        taskExecutor.queueCapacity = taskProperties.queueCapacity
        // 等待时间 （默认为0，此时立即停止）
        taskExecutor.setAwaitTerminationSeconds(taskProperties.awaitSeconds)
        // 空闲线程存活时间
        taskExecutor.keepAliveSeconds = taskProperties.keepAliveSeconds
        // 等待任务在关机时完成--表明等待所有线程执行完
        taskExecutor.setWaitForTasksToCompleteOnShutdown(taskProperties.isShutdown)
        // 线程池名称前缀
        taskExecutor.threadNamePrefix = taskProperties.threadNamePrefix
        // 线程池拒绝策略
        taskExecutor.setRejectedExecutionHandler(ThreadPoolExecutor.DiscardOldestPolicy())
        // 线程池初始化
        taskExecutor.initialize()
        logger.info("APP通知线程池初始化...")
        return taskExecutor
    }

    override fun getComponentName() = "kudos-ability-distributed-notify-rdb"

}
