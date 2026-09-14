package io.kudos.ms.tag.api.internal

import io.kudos.ms.tag.api.internal.controller.TagAttributeFactInternalController
import io.kudos.ms.tag.api.internal.controller.TagQueryInternalController
import io.kudos.ms.tag.client.init.TagClientAutoConfiguration
import io.kudos.ms.tag.client.proxy.ITagAttributeFactProxy
import io.kudos.ms.tag.client.proxy.ITagQueryProxy
import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.attribute.model.TagAttributeOperation
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.fact.api.ITagAttributeFactApi
import io.kudos.ms.tag.common.fact.model.TagAttributeFactResult
import io.kudos.ms.tag.common.fact.model.TagAttributeFactStatus
import io.kudos.ms.tag.common.query.api.ITagQueryApi
import io.kudos.ms.tag.common.query.model.TagQueryExpression
import io.kudos.ms.tag.common.query.model.TagQueryRequest
import io.kudos.ms.tag.common.query.model.TagSubjectPage
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import io.kudos.ms.tag.core.security.TagSubjectWriteGuard
import io.kudos.ms.tag.core.security.TagTenantAccessGuard
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.mockito.Mockito.mock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals

@EnableKudosTest(
    classes = [StandaloneTagTestApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = [
        "server.port=${StandaloneTagServiceTest.PORT}",
        "spring.http.serviceclient.${TagClientAutoConfiguration.GROUP}.base-url=http://localhost:${StandaloneTagServiceTest.PORT}",
        "spring.cloud.nacos.config.import-check.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
    ],
)
@Import(
    TagAttributeFactInternalController::class,
    TagQueryInternalController::class,
    StandaloneTagServiceTest.Configuration::class,
)
open class StandaloneTagServiceTest {

    @Autowired private lateinit var factClient: ITagAttributeFactProxy
    @Autowired private lateinit var queryClient: ITagQueryProxy

    @Test
    fun `standalone HTTP client keeps reusable tag results tenant isolated`() {
        factClient.submitFacts(
            listOf(
                ageFact("tenant-a", "same-id", 35, "event-a"),
                ageFact("tenant-b", "same-id", 50, "event-b"),
            )
        )

        assertEquals(listOf("same-id"), queryClient.findSubjects(ageQuery("tenant-a")).subjectIds)
        assertEquals(emptyList(), queryClient.findSubjects(ageQuery("tenant-b")).subjectIds)
    }

    @TestConfiguration
    open class Configuration {
        @Bean
        open fun inMemoryTagService() = InMemoryTagService()

        @Bean("tagAttributeFactApi")
        open fun tagAttributeFactApi(
            @Qualifier("inMemoryTagService") service: InMemoryTagService,
        ): ITagAttributeFactApi = service

        @Bean("tagQueryApi")
        open fun tagQueryApi(
            @Qualifier("inMemoryTagService") service: InMemoryTagService,
        ): ITagQueryApi = service

        @Bean
        open fun tagTenantAccessGuard() = object : TagTenantAccessGuard() {
            override fun requireTenant(tenantId: String) = Unit
        }

        @Bean
        open fun tagSubjectWriteGuard() = object : TagSubjectWriteGuard() {
            override fun requireWrite(key: TagSubjectKey) = Unit
        }

        @Bean
        open fun tagSubjectTypeDao(): TagSubjectTypeDao = mock(TagSubjectTypeDao::class.java)
    }

    open class InMemoryTagService : ITagAttributeFactApi, ITagQueryApi {
        private val materializedTags = ConcurrentHashMap<TagSubjectKey, Set<String>>()

        override fun submitFacts(facts: List<TagAttributeFact>): List<TagAttributeFactResult> = facts.map { fact ->
            val age = (fact.value as TagAttributeValue.IntegerValue).value
            materializedTags[fact.subjectKey] = if (age in 30L..40L) setOf(AGE_TAG) else emptySet()
            TagAttributeFactResult(fact.eventId, TagAttributeFactStatus.APPLIED, stateVersion = 1)
        }

        override fun findSubjects(request: TagQueryRequest): TagSubjectPage {
            val expression = request.expression as TagQueryExpression.AllTags
            require(expression.tagCodes == setOf(AGE_TAG))
            return TagSubjectPage(
                materializedTags.asSequence()
                    .filter { (key, tags) ->
                        key.tenantId == request.tenantId &&
                            key.subjectType == request.subjectType &&
                            tags.containsAll(expression.tagCodes)
                    }
                    .map { it.key.subjectId }
                    .sorted()
                    .take(request.pageSize)
                    .toList(),
            )
        }
    }

    companion object {
        const val PORT = "18110"
        private const val AGE_TAG = "age_30_40"

        private fun ageFact(tenantId: String, subjectId: String, age: Long, eventId: String) = TagAttributeFact(
            eventId = eventId,
            subjectKey = TagSubjectKey(tenantId, "hr.person", subjectId),
            attributeCode = "age",
            operation = TagAttributeOperation.SET,
            value = TagAttributeValue.IntegerValue(age),
            occurredAt = Instant.parse("2026-09-14T00:00:00Z"),
            sourceCode = "hr",
        )

        private fun ageQuery(tenantId: String) = TagQueryRequest(
            tenantId = tenantId,
            subjectType = "hr.person",
            expression = TagQueryExpression.AllTags(setOf(AGE_TAG)),
        )
    }
}

@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration(
    excludeName = [
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceAutoConfiguration",
    ],
)
@Import(TagClientAutoConfiguration::class)
open class StandaloneTagTestApplication
