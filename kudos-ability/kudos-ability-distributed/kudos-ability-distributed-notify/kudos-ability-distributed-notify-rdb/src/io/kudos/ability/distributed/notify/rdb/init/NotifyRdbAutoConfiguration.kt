package io.kudos.ability.distributed.notify.rdb.init

import jakarta.annotation.PostConstruct
import org.mybatis.spring.annotation.MapperScan
import org.soul.ability.distributed.notify.rdb.entity.SysApp
import org.soul.ability.distributed.notify.rdb.service.SysAppService
import org.soul.ability.distributed.notify.rdb.support.AppCheckServlet
import org.soul.ability.distributed.notify.rdb.support.AppNotifyServlet
import org.soul.ability.distributed.notify.rdb.support.TaskProperties
import org.soul.base.lang.string.StringTool
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.soul.context.core.SoulPropertySourceFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
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
 * @author Fei
 * @date 2022/12/20 11:58
 * @since 5.0.0
 */
@Configuration
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = ["org.soul.ability.distributed.notify.rdb"])
@PropertySource(
    value = ["classpath:soul-ability-distributed-notify-rdb.yml"],
    factory = SoulPropertySourceFactory::class
)
@MapperScan(value = ["org.soul.ability.distributed.notify.rdb.mapper"])
class NotifyRdbAutoConfiguration : ApplicationListener<WebServerInitializedEvent?>, AsyncConfigurer {
    @Autowired
    private val sysAppService: SysAppService? = null

    @Autowired
    private val taskProperties: TaskProperties? = null

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
        val result = sysAppService!!.addApp(app)
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
            val catalina = n.getDomain()
            if ("Catalina" == catalina) {
                return n.getKeyProperty("port").toInt()
            }
        }
        return 0
    }

    @Bean
    fun appCheckServlet(): ServletRegistrationBean<AppCheckServlet?> {
        val bean = ServletRegistrationBean<AppCheckServlet?>(AppCheckServlet(), "/errors/app-check.html")
        bean.setLoadOnStartup(1)
        return bean
    }

    @Bean
    fun appNotifyServlet(): ServletRegistrationBean<AppNotifyServlet?> {
        val bean = ServletRegistrationBean<AppNotifyServlet?>(AppNotifyServlet(), "/errors/app-notify.html")
        bean.setLoadOnStartup(1)
        return bean
    }

    @Bean("appNotifyExecutor")
    override fun getAsyncExecutor(): Executor {
        val taskExecutor = ThreadPoolTaskExecutor()
        // 配置核心线程池数量
        taskExecutor.setCorePoolSize(taskProperties!!.getCorePoolSize())
        // 配置最大线程池数量
        taskExecutor.setMaxPoolSize(taskProperties.getMaxPoolSize())
        /** 线程池所使用的缓冲队列 */
        taskExecutor.setQueueCapacity(taskProperties.getQueueCapacity())
        // 等待时间 （默认为0，此时立即停止）
        taskExecutor.setAwaitTerminationSeconds(taskProperties.getAwaitSeconds())
        // 空闲线程存活时间
        taskExecutor.setKeepAliveSeconds(taskProperties.getKeepAliveSeconds())
        // 等待任务在关机时完成--表明等待所有线程执行完
        taskExecutor.setWaitForTasksToCompleteOnShutdown(taskProperties.isShutdown())
        // 线程池名称前缀
        taskExecutor.setThreadNamePrefix(taskProperties.getThreadNamePrefix())
        // 线程池拒绝策略
        taskExecutor.setRejectedExecutionHandler(ThreadPoolExecutor.DiscardOldestPolicy())
        // 线程池初始化
        taskExecutor.initialize()
        logger.info("APP通知线程池初始化...")
        return taskExecutor
    }

    @PostConstruct
    fun init() {
        logger.info("[soul-ability-distributed-notify-rdb]初始化完成...")
    }

    companion object {
        private val logger: Log = LogFactory.getLog(NotifyRdbAutoConfiguration::class.java)
    }
}
