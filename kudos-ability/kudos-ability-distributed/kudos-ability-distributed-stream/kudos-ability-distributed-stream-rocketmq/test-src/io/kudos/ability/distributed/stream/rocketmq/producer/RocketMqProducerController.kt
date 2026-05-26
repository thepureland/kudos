package io.kudos.ability.distributed.stream.rocketmq.producer

import io.kudos.ability.distributed.stream.rocketmq.data.RocketMqSimpleMsg
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * RocketMQ test producer controller.
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/producer")
open class RocketMqProducerController {

    private val log = LogFactory.getLog(this::class)

    @Autowired
    private lateinit var producerService: IRocketMqProducerService

    /**
     * Send an MQ message.
     */
    @RequestMapping("/send")
    fun send(@RequestParam("message") message: String?) {
        log.info("########## send: $message")
        producerService.producer(RocketMqSimpleMsg(message))
    }

}
