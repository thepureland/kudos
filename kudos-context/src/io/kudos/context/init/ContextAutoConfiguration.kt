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
 * Context auto-configuration class.
 *
 * @author K
 * @since 1.0.0
 */
/**
 * `@Role(ROLE_INFRASTRUCTURE)`: this @Configuration implements [IComponentInitializer] and is taken over very early by
 * [ComponentInitializationDispatcher] (itself a BPP), so it is guaranteed to be created before business BPPs.
 * Without this marker, Spring emits the "is not eligible for getting processed by all BeanPostProcessors" warning.
 * Declaring it explicitly as an infrastructure bean tells Spring not to apply business BPPs / auto-proxy to it.
 */
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Import(ComponentInitializationDispatcher::class)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
open class ContextAutoConfiguration : IComponentInitializer {

    /**
     * Default failData task scheduler.
     *
     * [io.kudos.context.retry.FailedDataRetryScanner] injects this bean via `@Qualifier("failDataTaskScheduler")`.
     * Previously this bean was **not provided in kudos-context**, so the application had to declare a bean of the
     * same name to start, otherwise a `NoSuchBeanDefinitionException` was thrown.
     *
     * Now kudos-context provides a single-thread [ThreadPoolTaskScheduler] by default; applications that need higher
     * concurrency can override by declaring a bean of the same name (automatically yielding via
     * [ConditionalOnMissingBean]).
     */
    @Bean("failDataTaskScheduler")
    @ConditionalOnMissingBean(name = ["failDataTaskScheduler"])
    open fun failDataTaskScheduler(): TaskScheduler = ThreadPoolTaskScheduler().apply {
        poolSize = 1
        setThreadNamePrefix("kudos-failed-data-retry-")
        // daemon=true: JVM shutdown is not blocked by this thread pool
        setDaemon(true)
        setWaitForTasksToCompleteOnShutdown(false)
        initialize()
    }

    override fun getComponentName() = "kudos-context"

}