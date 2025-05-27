package io.kudos.ability.distributed.stream.kafka.main

interface IKafkaMainService {
    fun sendAndReceiveMessage(): String?

    fun errorMessage(): String?
}
