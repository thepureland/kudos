package io.kudos.ms.tag.core.platform.init

import io.kudos.ability.data.rdb.ktorm.init.KtormAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.tag.core.rule.engine.JvmTagRuleEvaluator
import io.kudos.ms.tag.core.runtime.port.AttributeStateStore
import io.kudos.ms.tag.core.runtime.port.RecalculationQueue
import io.kudos.ms.tag.core.runtime.port.TagAssignmentIndex
import io.kudos.ms.tag.core.runtime.port.TagMembershipStore
import io.kudos.ms.tag.core.runtime.port.TagRuleEvaluator
import io.kudos.ms.tag.core.runtime.rdb.RdbAttributeStateStore
import io.kudos.ms.tag.core.runtime.rdb.RdbRecalculationQueue
import io.kudos.ms.tag.core.runtime.rdb.RdbTagAssignmentIndex
import io.kudos.ms.tag.core.runtime.rdb.RdbTagMembershipStore
import io.kudos.ms.tag.core.runtime.job.dao.TagRecalculationJobDao
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

/**
 * Auto-configuration entry point for the embeddable tag atomic service.
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@ComponentScan(basePackages = ["io.kudos.ms.tag.core"])
@AutoConfigureAfter(KtormAutoConfiguration::class)
open class TagAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean(AttributeStateStore::class)
    open fun attributeStateStore(): AttributeStateStore = RdbAttributeStateStore()

    @Bean
    @ConditionalOnMissingBean(TagMembershipStore::class)
    open fun tagMembershipStore(): TagMembershipStore = RdbTagMembershipStore()

    @Bean
    @ConditionalOnMissingBean(TagAssignmentIndex::class)
    open fun tagAssignmentIndex(): TagAssignmentIndex = RdbTagAssignmentIndex()

    @Bean
    @ConditionalOnMissingBean(TagRuleEvaluator::class)
    open fun tagRuleEvaluator(): TagRuleEvaluator = JvmTagRuleEvaluator()

    @Bean
    @ConditionalOnMissingBean(RecalculationQueue::class)
    open fun recalculationQueue(jobDao: TagRecalculationJobDao): RecalculationQueue = RdbRecalculationQueue(jobDao)

    override fun getComponentName() = "kudos-ms-tag-core"
}
