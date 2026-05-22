package io.kudos.ability.distributed.stream.common.annotations

/**
 * Mq生产者注解
 *
 * @author paul
 * @author K
 * @author AI: Codex
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
    val topic: String = "",

    /**
     * 作为消息体发送的方法参数下标，默认取第一个参数。
     *
     * 多参数 producer 方法应显式指定，避免业务参数被静默忽略。
     */
    val payloadParameterIndex: Int = 0,

    /**
     * 方法返回 Boolean false 时是否取消发送，默认保持历史行为。
     */
    val cancelOnFalse: Boolean = true

)
