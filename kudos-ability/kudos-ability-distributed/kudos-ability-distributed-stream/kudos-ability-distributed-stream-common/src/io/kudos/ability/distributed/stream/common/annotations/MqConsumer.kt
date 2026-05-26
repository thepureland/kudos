package io.kudos.ability.distributed.stream.common.annotations

import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.AliasFor

/**
 * MQ consumer annotation.
 *
 * Note: methods annotated with this must return java.util.function.Consumer.
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Bean
annotation class MqConsumer(

    /**
     * Name of the bound stream consumer configuration, e.g. consumer-in-0.
     * Optional; provided only to make lookup easier.
     */
    @get:AliasFor(annotation = Bean::class, attribute = "name") val beanName: Array<String> = [],
    val bindingName: String = "",

    /**
     * Optional; provided only to make lookup easier.
     */
    val topic: String = ""

)
