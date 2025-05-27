package io.kudos.ability.distributed.stream.kafka.producer

import io.kudos.ability.distributed.stream.kafka.data.KafkaSimpleMsg
import org.soul.base.log.Log
import org.soul.base.log.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * kafka 生產者 controller
 *
 * @author shane
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/producer")
class KafkaProducerController {
    private val log: Log = LogFactory.getLog(KafkaProducerController::class.java)

    @Autowired
    private val producerService: IKafkaProducerService? = null

    /**
     * 發送mq信息
     */
    @RequestMapping("/send")
    fun send(@RequestParam("message") message: String?) {
        log.info("########## send: {0}", message)
        producerService!!.producer(KafkaSimpleMsg(message))
    }
}
