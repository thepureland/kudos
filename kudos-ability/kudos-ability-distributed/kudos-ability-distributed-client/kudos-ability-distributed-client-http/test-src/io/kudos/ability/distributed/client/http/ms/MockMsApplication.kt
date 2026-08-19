package io.kudos.ability.distributed.client.http.ms

import io.kudos.context.config.YamlPropertySourceFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource

/**
 * Mock microservice Application.
 *
 * Deliberately **not** `@EnableKudos` — see [MockMsSignatureVerificationConfiguration] for why, and for
 * how the provider half of context propagation is wired instead.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableDiscoveryClient
@SpringBootApplication
@Import(MockMsSignatureVerificationConfiguration::class)
@PropertySource(
    value = ["classpath:application-ms.yml"], factory = YamlPropertySourceFactory::class
)
open class MockMsApplication
