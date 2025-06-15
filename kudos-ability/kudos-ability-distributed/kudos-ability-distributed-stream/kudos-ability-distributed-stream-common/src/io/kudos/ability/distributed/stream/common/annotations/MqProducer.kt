package io.kudos.ability.distributed.stream.common.annotations

/**
 * Mq生产者注解
 *
 * @author paul
 * @author K
 * @since 1.0.0
 */
@Target(AnnotationTarget.FUNCTION)
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
