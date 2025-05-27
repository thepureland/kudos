package io.kudos.ability.distributed.stream.kafka.init

/**
 * @Description: Mq 生产者注解
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class MqProducer(
    /**
     * 绑定的stream生产者配置名，示例：producer-out-0
     */
    val bindingName: String,
    /**
     * 该字段为非必填，仅用于方便查找
     */
    val topic: String = ""
)
