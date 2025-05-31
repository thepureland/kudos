package io.kudos.ability.distributed.stream.common.annotations

import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.AliasFor

/**
 * Mq消费者注解
 *
 * 注意：该注解修饰的方法返回值，一定得是java.util.function.Consumer类型
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
     * 绑定的stream消费者配置名，示例：consumer-in-0
     * 注：该字段为非必填，仅用于方便查找
     */
    @get:AliasFor(annotation = Bean::class, attribute = "name") val beanName: Array<String> = [],
    val bindingName: String = "",

    /**
     * 该字段为非必填，仅用于方便查找
     */
    val topic: String = ""

)
