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
import io.kudos.ms.tag.core.runtime.attribute.dao.TagAttributeStateDao
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.runtime.subject.dao.TagSubjectDao
import io.kudos.ms.tag.core.runtime.membership.dao.TagMembershipDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentDao
import io.kudos.ms.tag.core.runtime.assignment.dao.TagAssignmentEventDao
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import io.kudos.ms.tag.core.runtime.job.TagRecalculationProperties
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
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
@EnableConfigurationProperties(TagRecalculationProperties::class)
open class TagAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean(AttributeStateStore::class)
    open fun attributeStateStore(
        stateDao: TagAttributeStateDao,
        attributeDao: TagAttributeDefinitionDao,
        subjectDao: TagSubjectDao,
    ): AttributeStateStore = RdbAttributeStateStore(stateDao, attributeDao, subjectDao)

    @Bean
    @ConditionalOnMissingBean(TagMembershipStore::class)
    open fun tagMembershipStore(membershipDao: TagMembershipDao): TagMembershipStore = RdbTagMembershipStore(membershipDao)

    @Bean
    @ConditionalOnMissingBean(TagAssignmentIndex::class)
    open fun tagAssignmentIndex(
        assignmentDao: TagAssignmentDao,
        assignmentEventDao: TagAssignmentEventDao,
        tagDao: TagDefinitionDao,
        tagSetDao: TagSetDao,
    ): TagAssignmentIndex = RdbTagAssignmentIndex(assignmentDao, assignmentEventDao, tagDao, tagSetDao)

    @Bean
    @ConditionalOnMissingBean(TagRuleEvaluator::class)
    open fun tagRuleEvaluator(): TagRuleEvaluator = JvmTagRuleEvaluator()

    @Bean
    @ConditionalOnMissingBean(RecalculationQueue::class)
    open fun recalculationQueue(
        jobDao: TagRecalculationJobDao,
        properties: TagRecalculationProperties,
    ): RecalculationQueue = RdbRecalculationQueue(jobDao, maximumAttempts = properties.maximumAttempts)

    override fun getComponentName() = "kudos-ms-tag-core"
}
