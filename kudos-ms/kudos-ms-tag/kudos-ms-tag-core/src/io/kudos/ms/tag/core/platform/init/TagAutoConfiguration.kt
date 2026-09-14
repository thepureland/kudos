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
import io.kudos.ms.tag.core.runtime.job.TagJobHealthSnapshot
import io.kudos.ms.tag.core.runtime.job.TagRuntimeHealthIndicator
import io.kudos.ms.tag.core.runtime.metrics.TagRuntimeMetrics
import io.kudos.ms.tag.core.rule.dao.TagRuleDao
import org.flywaydb.core.Flyway
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.sql.DataSource

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

    /** Optional metrics bridge; the core remains loadable without Micrometer. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = ["io.micrometer.core.instrument.MeterRegistry"])
    open class MicrometerConfiguration {
        @Bean
        @ConditionalOnBean(type = ["io.micrometer.core.instrument.MeterRegistry"])
        @ConditionalOnMissingBean(TagRuntimeMetrics::class)
        open fun tagRuntimeMetrics(
            registry: io.micrometer.core.instrument.MeterRegistry,
        ): TagRuntimeMetrics = TagRuntimeMetrics(registry)
    }

    /** Optional Actuator bridge assembled only when the host exposes a DataSource and health SPI. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = ["org.springframework.boot.health.contributor.HealthIndicator"])
    open class HealthConfiguration {
        @Bean("tagRuntimeHealthIndicator")
        @ConditionalOnBean(DataSource::class)
        @ConditionalOnMissingBean(TagRuntimeHealthIndicator::class)
        open fun tagRuntimeHealthIndicator(
            dataSource: DataSource,
            jobDao: TagRecalculationJobDao,
            ruleDao: TagRuleDao,
            flywayProvider: ObjectProvider<Flyway>,
        ): TagRuntimeHealthIndicator = TagRuntimeHealthIndicator(
            rdbProbe = { dataSource.connection.use { it.isValid(2) } },
            jobProbe = { instant ->
                val now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
                val jobs = jobDao.listAll()
                val oldestPending = jobs
                    .filter { it.status == "PENDING" || it.status == "RETRY_WAIT" }
                    .minOfOrNull { it.createTime }
                    ?.let { Duration.between(it, now).coerceAtLeast(Duration.ZERO) }
                TagJobHealthSnapshot(
                    activeWorkerLeases = jobs.count {
                        it.status == "RUNNING" && it.leaseUntil?.isAfter(now) == true
                    },
                    oldestPendingJobAge = oldestPending,
                    failedJobCount = jobs.count { it.status == "FAILED" }.toLong(),
                )
            },
            publishedRuleProbe = {
                ruleDao.listPublished().all { rule ->
                    val tree = ruleDao.loadTree(rule.tenantId, rule.id, rule.ruleVersion)
                    tree != null && rule.rootNodeId != null && tree.nodes.any { it.id == rule.rootNodeId }
                }
            },
            flywayVersionProbe = {
                flywayProvider.ifAvailable?.info()?.current()?.version?.toString()
            },
        )
    }
}
