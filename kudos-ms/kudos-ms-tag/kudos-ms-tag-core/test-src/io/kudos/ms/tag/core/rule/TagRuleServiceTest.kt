package io.kudos.ms.tag.core.rule

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeType
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.catalog.model.CreateTagAttributeCommand
import io.kudos.ms.tag.common.catalog.model.CreateTagCommand
import io.kudos.ms.tag.common.catalog.model.RegisterTagSubjectTypeCommand
import io.kudos.ms.tag.common.rule.model.TagRuleExpression
import io.kudos.ms.tag.common.rule.model.TagRuleOperator
import io.kudos.ms.tag.core.TagDaoTestSupport
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.service.impl.TagCatalogService
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import io.kudos.ms.tag.core.rule.dao.TagRuleDependencyDao
import io.kudos.ms.tag.core.rule.dao.TagRuleDao
import io.kudos.ms.tag.core.rule.dao.TagRuleNodeDao
import io.kudos.ms.tag.core.rule.dao.TagRuleOperandDao
import io.kudos.ms.tag.core.rule.model.TagRuleStatus
import io.kudos.ms.tag.core.rule.service.impl.TagRuleService
import io.kudos.ms.tag.core.rule.validation.TagRuleValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class TagRuleServiceTest : TagDaoTestSupport() {

    private val attributeDao = TagAttributeDefinitionDao()
    private val tagDao = TagDefinitionDao()
    private val ruleDao = TagRuleDao()
    private val nodeDao = TagRuleNodeDao()
    private val operandDao = TagRuleOperandDao()
    private val dependencyDao = TagRuleDependencyDao()
    private val catalog = TagCatalogService(TagSubjectTypeDao(), attributeDao, TagSetDao(), tagDao)
    private val service = TagRuleService(
        tagDao,
        attributeDao,
        ruleDao,
        nodeDao,
        operandDao,
        dependencyDao,
        TagRuleValidator(attributeDao, tagDao, ruleDao, dependencyDao),
    )

    @Test
    fun publishesImmutableVersionAndThenCreatesTheNextDraft() {
        prepareCatalog()
        val v1Expression = between(30, 40)
        val draft = service.createDraft(TENANT, "working_age", v1Expression)
        assertEquals(1, draft.ruleVersion)
        assertEquals(TagRuleStatus.DRAFT, draft.status)
        assertEquals(2, ruleDao.loadTree(TENANT, draft.id, 1)?.operands?.size)
        assertEquals(1, dependencyDao.listByRule(draft.id).size)

        val replacement = between(31, 41)
        val replaced = service.replaceDraft(TENANT, draft.id, replacement)
        assertEquals(draft.id, replaced.id)
        assertEquals(replacement, replaced.expression)
        assertEquals(1, dependencyDao.listByRule(draft.id).size)

        val publication = service.requestPublication(TENANT, draft.id)
        assertEquals(TagRuleStatus.REBUILDING, publication.rule.status)
        assertEquals(64, publication.rule.checksum.length)
        assertFailsWith<IllegalArgumentException> {
            service.replaceDraft(TENANT, draft.id, between(32, 42))
        }

        val published = service.completePublication(TENANT, draft.id)
        assertEquals(TagRuleStatus.PUBLISHED, published.status)
        assertEquals(replacement, service.getPublished(TENANT, "working_age")?.expression)

        val v2 = service.createDraft(TENANT, "working_age", between(32, 42))
        assertEquals(2, v2.ruleVersion)
        assertNotEquals(draft.id, v2.id)
        assertTrue(ruleDao.loadTree(TENANT, draft.id, 1)?.nodes?.isNotEmpty() == true)
    }

    private fun prepareCatalog() {
        catalog.registerSubjectType(RegisterTagSubjectTypeCommand(SUBJECT, "Person", "hr"))
        catalog.createAttribute(CreateTagAttributeCommand(TENANT, SUBJECT, "age", "Age", TagAttributeType.INTEGER, TagAttributeCardinality.SINGLE))
        catalog.createTag(CreateTagCommand(TENANT, SUBJECT, "working_age", "Working age", manualAssignable = false))
    }

    private fun between(lower: Long, upper: Long) = TagRuleExpression.AttributePredicate(
        "age",
        TagRuleOperator.BETWEEN,
        listOf(TagAttributeValue.IntegerValue(lower), TagAttributeValue.IntegerValue(upper)),
    )

    private companion object {
        const val TENANT = "tenant-a"
        const val SUBJECT = "hr.person"
    }
}
