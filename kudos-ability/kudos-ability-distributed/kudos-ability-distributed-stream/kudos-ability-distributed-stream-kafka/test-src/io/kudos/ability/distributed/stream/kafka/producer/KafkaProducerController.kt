package io.kudos.ability.distributed.stream.kafka.producer

import io.kudos.ability.distributed.stream.kafka.data.KafkaSimpleMsg
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * kafka测试 生產者 controller
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/producer")
open class KafkaProducerController {

    private val log = LogFactory.getLog(this)

    @Autowired
    private lateinit var producerService: IKafkaProducerService

    /**
     * 發送mq信息
     */
    @RequestMapping("/send")
    fun send(@RequestParam("message") message: String?) {
        log.info("########## send: $message")
        producerService.producer(KafkaSimpleMsg(message))
    }

}
