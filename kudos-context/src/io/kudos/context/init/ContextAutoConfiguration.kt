package io.kudos.context.init

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Role
import org.springframework.core.Ordered
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler


/**
 * 上下文自动配置类
 *
 * @author K
 * @since 1.0.0
 */
/**
 * `@Role(ROLE_INFRASTRUCTURE)`: 此 @Configuration 实现了 [IComponentInitializer]，由
 * [ComponentInitializationDispatcher]（本身是 BPP）在很早期接管，因此一定会先于业务 BPP 完成创建。
 * 没有这个标记时，Spring 会发 "is not eligible for getting processed by all BeanPostProcessors" 警告。
 * 这里显式声明为基础设施 bean，告诉 Spring 不需要给它套业务 BPP/auto-proxy。
 */
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Import(ComponentInitializationDispatcher::class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
open class ContextAutoConfiguration : IComponentInitializer {

    /**
     * 默认的 failData 任务调度器。
     *
     * [io.kudos.context.retry.FailedDataRetryScanner] 通过 `@Qualifier("failDataTaskScheduler")`
     * 注入此 Bean。之前此 Bean **未在 kudos-context 提供**，应用必须自己声明同名 Bean 才能启动，
     * 否则报 `NoSuchBeanDefinitionException`。
     *
     * 现在 kudos-context 默认提供单线程 [ThreadPoolTaskScheduler]，应用如需更高并发可声明
     * 同名 Bean 覆盖（被 [ConditionalOnMissingBean] 自动让位）。
     */
    @Bean("failDataTaskScheduler")
    @ConditionalOnMissingBean(name = ["failDataTaskScheduler"])
    open fun failDataTaskScheduler(): TaskScheduler = ThreadPoolTaskScheduler().apply {
        poolSize = 1
        setThreadNamePrefix("kudos-failed-data-retry-")
        // daemon=true：JVM 关闭不被该线程池阻塞
        setDaemon(true)
        setWaitForTasksToCompleteOnShutdown(false)
        initialize()
    }

    override fun getComponentName() = "kudos-context"

}