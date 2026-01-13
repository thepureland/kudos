package io.kudos.ability.distributed.stream.common.support

import io.kudos.ability.distributed.stream.common.handler.StreamFailHandlerItem
import io.kudos.ability.distributed.stream.common.model.vo.StreamHeader
import io.kudos.ability.distributed.stream.common.model.vo.StreamMessageVo
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cloud.stream.config.BindingServiceProperties
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandlingException
import org.springframework.messaging.support.ErrorMessage
import org.springframework.messaging.support.GenericMessage
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * 流式消息生产者辅助类
 * 
 * 提供消息发送的统一接口，支持同步和异步发送，以及失败处理和重试机制。
 * 
 * 核心功能：
 * 1. 同步发送：调用sendMessage方法，立即返回发送结果
 * 2. 异步发送：调用asyncSendMessage方法，使用线程池异步执行，不阻塞调用线程
 * 3. 失败处理：当消息发送失败时，如果配置了失败处理器，会将失败消息发送到错误通道进行后续处理
 * 4. 消息封装：自动创建StreamMessageVo消息对象，并添加必要的消息头信息（包括绑定名称、目标地址等）
 * 
 * 注意事项：
 * - MQ发送都是异步的，send方法返回true不代表消息已成功发送到MQ服务器
 * - 真实的发送错误会在flush时感知，此时会触发失败处理机制
 * - 支持通过StreamFailHandlerItem注册自定义的失败处理器
 */
class StreamProducerHelper {

    @Autowired
    private lateinit var streamBridge: StreamBridge

    @Autowired
    private lateinit var properties: BindingServiceProperties

    @Autowired
    private lateinit var streamAsyncSendExecutor: ThreadPoolTaskExecutor

    @Autowired
    @Qualifier("mqProducerChannel")
    private lateinit var mqProducerChannel: MessageChannel

    /**
     * 发送消息
     *
     * @param bindingName stream配置名
     * @param data        消息体对象
     * @param <T>         消息体对象类型
    </T> */
    fun <T> sendMessage(bindingName: String, data: T): Boolean {
        if (!properties.bindings.containsKey(bindingName)) {
            LOG.error("未找到Stream配置项{0}", bindingName)
            return false
        }
        val msg = createMessage(bindingName, data)
        @Suppress("UNCHECKED_CAST")
        val success = doRealSend(bindingName, msg as Message<StreamMessageVo<Any?>>, false)
        if (!success) {
            LOG.warn("stream发送消息结果:false, bindingName:${bindingName}, msgId:${msg.headers.id}")
        }
        return success
    }

    /**
     * 发送消息
     *
     * @param bindingName stream配置名
     * @param data        消息体对象
     * @param <T>         消息体对象类型
    </T> */
    fun <T> asyncSendMessage(bindingName: String, data: T) {
        if (!properties.getBindings().containsKey(bindingName)) {
            LOG.error("未找到Stream配置项{0}", bindingName)
            return
        }
        val msg = createMessage(bindingName, data)
        streamAsyncSendExecutor.execute {
            @Suppress("UNCHECKED_CAST")
            val success = doRealSend(bindingName, msg as Message<StreamMessageVo<Any?>>, false)
            if (!success) {
                LOG.warn("stream发送消息结果:false, bindingName:${bindingName}, msgId:${msg.headers.id}")
            }
        }
    }

    /**
     * 真实发送mq信息
     * @param bindingName
     * @param msg
     * @param isResend
     * @return
     * @param T
     */
    fun <T> doRealSend(bindingName: String, msg: Message<StreamMessageVo<T>>, isResend: Boolean): Boolean {
        try {
            //这里MQ都是异步发送，所以永远都会返回true，只有真实flush的是后才会感知错误信息
            return streamBridge.send(bindingName, msg, StreamMessageConverter.MESSAGE_TYPE)
        } catch (e: Throwable) {
            //避免乱七八糟错误补救而已
            LOG.error(e, "发送mq失败！")
            //兼容配置不开启异常处理的情况。重发失败返回false，则不需要删除
            //properties.getBindings().get(bindingName).getProducer().isErrorChannelEnabled();
            if (StreamFailHandlerItem.hasFailedHandler(bindingName) && !isResend) {
                val exception = MessageHandlingException(msg)
                val data = ErrorMessage(exception)
                mqProducerChannel.send(data)
            }
            return false
        }
    }

    private fun <T> createMessage(bindingName: String, data: T): Message<StreamMessageVo<T>> {
        val destination = properties.bindings[bindingName]!!.destination
        val header = StreamHeader.initHeader(destination)
        val headerMap = BeanKit.extract(header)
        val map = mutableMapOf<String, Any>()
        @Suppress("UNCHECKED_CAST")
        map.putAll(headerMap as Map<out String, Any>)
        map.put(StreamHeader.SCST_BIND_NAME, bindingName)
        val headers = org.springframework.messaging.MessageHeaders(map)
        val streamMessageVo = StreamMessageVo(data)
        return GenericMessage<StreamMessageVo<T>>(streamMessageVo, headers)
    }

    private val LOG = LogFactory.getLog(this)

}
