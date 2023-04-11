package io.kudos.ability.cache.interservice.provider

import io.kudos.context.init.EnableKudos
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication


@EnableKudos
@SpringBootApplication(scanBasePackages = ["io.kudos.ability.cache.interservice.provider"])
open class ProviderApplication {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(ProviderApplication::class.java, *args)
        }
    }

}