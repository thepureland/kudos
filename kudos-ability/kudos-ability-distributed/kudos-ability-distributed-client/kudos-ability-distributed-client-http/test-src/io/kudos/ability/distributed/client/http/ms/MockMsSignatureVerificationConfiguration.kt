package io.kudos.ability.distributed.client.http.ms

import io.kudos.ability.distributed.discovery.nacos.filter.InternalRpcContextSignatureVerifier
import io.kudos.ability.distributed.discovery.nacos.filter.InternalRpcContextWebFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/**
 * Registers the provider half of context propagation on the mock microservice: the context filter plus
 * a signature verifier sharing the client's secret.
 *
 * **Why this is hand-wired instead of `@EnableKudos`.** In production these beans come from
 * `NacosDiscoveryAutoConfiguration`, which `@EnableKudos` imports. But this test runs two Spring
 * applications in one JVM, and `@EnableKudos` on the second one overwrites the JVM-wide
 * `SpringKit.applicationContext` — after which the *client's* `KudosContextRequestInterceptor` resolves
 * its `IHttpRequestContextProcess` beans out of the provider's context and its trace-key write-back
 * lands nowhere the caller can see. Importing just these two beans gets the verification under test
 * without hijacking the client side.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
open class MockMsSignatureVerificationConfiguration {

    @Bean
    open fun mockInternalRpcContextWebFilterRegistration(): FilterRegistrationBean<InternalRpcContextWebFilter> =
        FilterRegistrationBean<InternalRpcContextWebFilter>().apply {
            setFilter(
                InternalRpcContextWebFilter(
                    false,
                    InternalRpcContextSignatureVerifier(secret = SHARED_SECRET)
                )
            )
            addUrlPatterns("/*")
            order = Ordered.HIGHEST_PRECEDENCE + 100
            setName("mockInternalRpcContextWebFilter")
        }

    companion object {
        /** Must match `kudos.ability.distributed.client.http.contextSignatureSecret` in application-client.yml. */
        const val SHARED_SECRET: String = "e2e-shared-secret"
    }
}
