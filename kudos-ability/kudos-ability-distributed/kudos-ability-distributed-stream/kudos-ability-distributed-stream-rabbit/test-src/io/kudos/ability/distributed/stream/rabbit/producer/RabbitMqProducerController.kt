package io.kudos.ability.distributed.stream.rabbit.producer

import io.kudos.ability.distributed.stream.rabbit.data.RabbitMqSimpleMsg
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * RabbitMq测试 生產者 controller
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/producer")
open class RabbitMqProducerController {

    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var producerService: IRabbitMqProducerService

    /**
     * 發送mq信息
     */
    @RequestMapping("/send")
    fun send(@RequestParam("message") message: String?) {
        log.info("########## send: $message")
        producerService.producer(RabbitMqSimpleMsg(message))
    }

}
