package io.kudos.ms.tag.core.runtime

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeFact
import io.kudos.ms.tag.common.attribute.model.TagAttributeOperation
import io.kudos.ms.tag.common.attribute.model.TagAttributeType
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.catalog.model.CreateTagAttributeCommand
import io.kudos.ms.tag.common.catalog.model.RegisterTagSubjectTypeCommand
import io.kudos.ms.tag.common.subject.model.TagSubjectKey
import io.kudos.ms.tag.core.TagDaoTestSupport
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.service.impl.TagCatalogService
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import io.kudos.ms.tag.core.fact.service.impl.TagAttributeFactService
import io.kudos.ms.tag.core.fact.service.impl.TagFactTransactionExecutor
import io.kudos.ms.tag.core.rule.dao.TagRuleDependencyDao
import io.kudos.ms.tag.core.rule.dao.TagRuleDao
import io.kudos.ms.tag.core.runtime.attribute.dao.TagAttributeEventDao
import io.kudos.ms.tag.core.runtime.attribute.dao.TagAttributeStateDao
import io.kudos.ms.tag.core.runtime.job.dao.TagRecalculationJobDao
import io.kudos.ms.tag.core.runtime.port.AttributeSourceBatch
import io.kudos.ms.tag.core.runtime.port.AttributeSourceConfig
import io.kudos.ms.tag.core.runtime.port.AttributeSourceProvider
import io.kudos.ms.tag.core.runtime.port.SourceCursor
import io.kudos.ms.tag.core.runtime.port.ValidationError
import io.kudos.ms.tag.core.runtime.rdb.RdbAttributeStateStore
import io.kudos.ms.tag.core.runtime.rdb.RdbRecalculationQueue
import io.kudos.ms.tag.core.runtime.service.impl.AttributeSourceRefreshService
import io.kudos.ms.tag.core.runtime.subject.dao.TagSubjectDao
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class AttributeSourceRefreshServiceTest : TagDaoTestSupport() {

    @Test
    fun providerCursorRemainsOpaqueAndFetchedFactsUseTheNormalIngestionPath() {
        val subjectTypeDao = TagSubjectTypeDao()
        val attributeDao = TagAttributeDefinitionDao()
        val subjectDao = TagSubjectDao()
        TagCatalogService(subjectTypeDao, attributeDao, TagSetDao(), TagDefinitionDao()).also { catalog ->
            catalog.registerSubjectType(RegisterTagSubjectTypeCommand(SUBJECT_TYPE, "Person", "hr"))
            catalog.createAttribute(
                CreateTagAttributeCommand(
                    TENANT,
                    SUBJECT_TYPE,
                    "age",
                    "Age",
                    TagAttributeType.INTEGER,
                    TagAttributeCardinality.SINGLE,
                )
            )
        }
        val stateStore = RdbAttributeStateStore(TagAttributeStateDao(), attributeDao, subjectDao)
        val factService = TagAttributeFactService(
            subjectTypeDao,
            attributeDao,
            TagAttributeEventDao(),
            subjectDao,
            stateStore,
            TagRuleDependencyDao(),
            TagRuleDao(),
            RdbRecalculationQueue(TagRecalculationJobDao()),
            TagFactTransactionExecutor(),
        )
        var receivedCursor: SourceCursor? = null
        val provider = object : AttributeSourceProvider {
            override val providerCode = "hr-feed"
            override fun validate(config: AttributeSourceConfig): List<ValidationError> = emptyList()
            override fun fetch(config: AttributeSourceConfig, cursor: SourceCursor?, limit: Int): AttributeSourceBatch {
                receivedCursor = cursor
                return AttributeSourceBatch(
                    listOf(
                        TagAttributeFact(
                            "provider-age",
                            TagSubjectKey(TENANT, SUBJECT_TYPE, "person-1"),
                            "age",
                            TagAttributeOperation.SET,
                            TagAttributeValue.IntegerValue(35),
                            Instant.parse("2026-09-14T00:00:00Z"),
                            providerCode,
                        )
                    ),
                    SourceCursor("opaque:next#2"),
                    exhausted = false,
                )
            }
        }
        val service = AttributeSourceRefreshService(listOf(provider), factService)

        val result = service.refresh(
            "hr-feed",
            AttributeSourceConfig(TENANT, SUBJECT_TYPE),
            SourceCursor("opaque:start#1"),
            50,
        )

        assertEquals(SourceCursor("opaque:start#1"), receivedCursor)
        assertEquals(SourceCursor("opaque:next#2"), result.nextCursor)
        assertEquals(
            listOf(TagAttributeValue.IntegerValue(35)),
            stateStore.load(TagSubjectKey(TENANT, SUBJECT_TYPE, "person-1"), setOf("age"))["age"],
        )
    }

    @Test
    fun providerCodesMustBeUniqueAndNoProviderIsRequired() {
        val duplicate = object : AttributeSourceProvider {
            override val providerCode = "duplicate"
            override fun validate(config: AttributeSourceConfig) = emptyList<ValidationError>()
            override fun fetch(config: AttributeSourceConfig, cursor: SourceCursor?, limit: Int) =
                AttributeSourceBatch(emptyList(), null, true)
        }
        val factService = TagAttributeFactService(
            TagSubjectTypeDao(),
            TagAttributeDefinitionDao(),
            TagAttributeEventDao(),
            TagSubjectDao(),
            RdbAttributeStateStore(TagAttributeStateDao(), TagAttributeDefinitionDao(), TagSubjectDao()),
            TagRuleDependencyDao(),
            TagRuleDao(),
            RdbRecalculationQueue(TagRecalculationJobDao()),
            TagFactTransactionExecutor(),
        )

        AttributeSourceRefreshService(emptyList(), factService)
        assertFailsWith<IllegalArgumentException> {
            AttributeSourceRefreshService(listOf(duplicate, duplicate), factService)
        }
    }

    private companion object {
        const val TENANT = "tenant-a"
        const val SUBJECT_TYPE = "hr.person"
    }
}
