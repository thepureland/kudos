package io.kudos.ability.distributed.stream.kafka.init

import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.AliasFor

/**
 * @Description: Mq 消费者注解
 * 注意：该注解修饰的方法返回值，一定得是java.util.function.Consumer接口修饰的值
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@Bean
annotation class MqConsumer(

    @get:AliasFor(annotation = Bean::class, attribute = "name") val beanName: Array<String> = [],
    /**
     * 绑定的stream消费者配置名，示例：consumer-in-0
     * 注：该字段为非必填，仅用于方便查找
     */
    val bindingName: String = "",
    /**
     * 该字段为非必填，仅用于方便查找
     */
    val topic: String = ""
)
