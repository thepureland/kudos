package io.kudos.ms.auth.session.spring.init

import io.kudos.ms.auth.core.authentication.session.spi.IAuthenticationContainerSessionPurger
import io.kudos.ms.auth.session.spring.SpringSessionContainerSessionPurger
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.Session

@AutoConfiguration
@ConditionalOnProperty(
    prefix = "kudos.ms.auth.session.container-purge",
    name = ["enabled"],
    havingValue = "true",
)
open class AuthSpringSessionPurgeAutoConfiguration {

    /**
     * Requires a principal-indexed repository rather than any `SessionRepository`: without the index there is
     * no way to find a user's container sessions, and a purger that silently finds nothing is worse than not
     * installing one, because the deployment believes revocation cleans up when it does not.
     */
    @Bean
    @ConditionalOnBean(FindByIndexNameSessionRepository::class)
    @ConditionalOnMissingBean(IAuthenticationContainerSessionPurger::class)
    open fun springSessionContainerSessionPurger(
        sessionRepository: FindByIndexNameSessionRepository<out Session>,
    ): IAuthenticationContainerSessionPurger = SpringSessionContainerSessionPurger(sessionRepository)
}
