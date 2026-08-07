package io.kudos.ms.auth.core.platform.init

import io.kudos.ability.data.rdb.ktorm.init.KtormAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.auth.core.platform.authz.condition.DefaultConditionEvaluator
import io.kudos.ms.auth.core.platform.authz.condition.IConditionEvaluator
import io.kudos.ms.auth.core.platform.authz.condition.IConditionClauseEvaluator
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration


/**
 * Auto-configuration class for the auth atomic service.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Cursor
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.auth.core"])
@AutoConfigureAfter(KtormAutoConfiguration::class)
open class AuthAutoConfiguration : IComponentInitializer {

    /**
     * The built-in condition evaluator, declared here rather than component-scanned so that
     * `@ConditionalOnMissingBean` is actually honoured — on a scanned `@Component` the condition is
     * evaluated before the application's own beans are known, and the override would not take.
     */
    @Bean
    @ConditionalOnMissingBean(IConditionEvaluator::class)
    open fun defaultConditionEvaluator(
        clauseEvaluators: ObjectProvider<IConditionClauseEvaluator>,
    ): IConditionEvaluator = DefaultConditionEvaluator(clauseEvaluators.orderedStream().toList())

    override fun getComponentName() = "kudos-ms-auth-core"

}
