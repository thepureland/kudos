package io.kudos.ms.msg.client.init

import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * msg Feign client auto-configuration.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(FeignClient::class)
@EnableFeignClients(basePackages = ["io.kudos.ms.msg.client"])
@ComponentScan(basePackages = ["io.kudos.ms.msg.client"])
open class MsgClientAutoConfiguration : IComponentInitializer {

    override fun getComponentName() = "kudos-ms-msg-client"

}
