package io.kudos.ms.tag.core.rule

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeType
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.catalog.model.CreateTagAttributeCommand
import io.kudos.ms.tag.common.catalog.model.CreateTagCommand
import io.kudos.ms.tag.common.catalog.model.RegisterTagSubjectTypeCommand
import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.common.error.TagValidationException
import io.kudos.ms.tag.common.rule.model.TagRuleExpression
import io.kudos.ms.tag.common.rule.model.TagRuleOperator
import io.kudos.ms.tag.core.TagDaoTestSupport
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.service.impl.TagCatalogService
import io.kudos.ms.tag.core.catalog.subjecttype.dao.TagSubjectTypeDao
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tag.model.po.TagDefinition
import io.kudos.ms.tag.core.catalog.tagset.dao.TagSetDao
import io.kudos.ms.tag.core.rule.dao.TagRuleDependencyDao
import io.kudos.ms.tag.core.rule.dao.TagRuleDao
import io.kudos.ms.tag.core.rule.model.po.TagRuleNode
import io.kudos.ms.tag.core.rule.model.po.TagRuleOperand
import io.kudos.ms.tag.core.rule.validation.TagRuleDependencyGraph
import io.kudos.ms.tag.core.rule.validation.TagRuleValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class TagRuleValidatorTest : TagDaoTestSupport() {

    private val attributeDao = TagAttributeDefinitionDao()
    private val tagDao = TagDefinitionDao()
    private val catalog = TagCatalogService(TagSubjectTypeDao(), attributeDao, TagSetDao(), tagDao)
    private val validator = TagRuleValidator(attributeDao, tagDao, TagRuleDao(), TagRuleDependencyDao())

    @Test
    fun rejectsOperatorsAndOperandsThatDoNotMatchTheAttributeType() {
        val target = prepareCatalog()

        assertCode(TagErrorCode.RULE_OPERATOR_TYPE_MISMATCH) {
            validator.validate(TENANT, target, predicate("enabled", TagRuleOperator.GT, TagAttributeValue.BooleanValue(true)))
        }
        assertCode(TagErrorCode.RULE_OPERAND_TYPE_MISMATCH) {
            validator.validate(TENANT, target, predicate("age", TagRuleOperator.EQ, TagAttributeValue.StringValue("35")))
        }
        assertCode(TagErrorCode.RULE_OPERAND_ORDER_INVALID) {
            validator.validate(
                TENANT,
                target,
                TagRuleExpression.AttributePredicate(
                    "age",
                    TagRuleOperator.BETWEEN,
                    listOf(TagAttributeValue.IntegerValue(40), TagAttributeValue.IntegerValue(30)),
                ),
            )
        }

        validator.validate(TENANT, target, predicate("name", TagRuleOperator.CONTAINS, TagAttributeValue.StringValue("lin")))
        validator.validate(TENANT, target, TagRuleExpression.AttributePredicate("age", TagRuleOperator.EXISTS, emptyList()))
        validator.validate(
            TENANT,
            target,
            TagRuleExpression.AttributePredicate(
                "age",
                TagRuleOperator.IN,
                listOf(TagAttributeValue.IntegerValue(30), TagAttributeValue.IntegerValue(40)),
            ),
        )
    }

    @Test
    fun rejectsAttributeAndTagReferencesOutsideTheTargetScope() {
        val target = prepareCatalog()

        assertCode(TagErrorCode.RULE_REFERENCE_NOT_FOUND) {
            validator.validate(TENANT, target, predicate("foreign_age", TagRuleOperator.EQ, TagAttributeValue.IntegerValue(35)))
        }
        assertCode(TagErrorCode.RULE_REFERENCE_NOT_FOUND) {
            validator.validate(TENANT, target, TagRuleExpression.HasTag("foreign_tag"))
        }
    }

    @Test
    fun rejectsMalformedPersistedPositionsAndOperandShapes() {
        val root = node("root", null, "ALL_OF", 0)
        val first = node("first", root.id, "ATTRIBUTE_PREDICATE", 0, operator = "EQ")
        val duplicate = node("second", root.id, "ATTRIBUTE_PREDICATE", 0, operator = "EQ")

        assertCode(TagErrorCode.RULE_STRUCTURE_INVALID) {
            validator.validateStructure(root.id, listOf(root, first, duplicate), emptyList())
        }

        val validPosition = node("second", root.id, "ATTRIBUTE_PREDICATE", 1, operator = "EQ")
        val tooManyOperands = listOf(operand("op-1", first.id, 0), operand("op-2", first.id, 1))
        assertCode(TagErrorCode.INVALID_OPERAND_COUNT) {
            validator.validateStructure(root.id, listOf(root, first, validPosition), tooManyOperands)
        }
    }

    @Test
    fun enforcesMaximumDepthAndNodeCount() {
        val target = prepareCatalog()
        val leaf = predicate("age", TagRuleOperator.EQ, TagAttributeValue.IntegerValue(35))
        val depthTwenty = (1 until 20).fold(leaf as TagRuleExpression) { child, _ -> TagRuleExpression.Not(child) }
        validator.validate(TENANT, target, depthTwenty)

        val depthTwentyOne = TagRuleExpression.Not(depthTwenty)
        assertCode(TagErrorCode.RULE_LIMIT_EXCEEDED) { validator.validate(TENANT, target, depthTwentyOne) }

        val fiveHundredOneNodes = TagRuleExpression.AllOf(List(500) { leaf })
        assertCode(TagErrorCode.RULE_LIMIT_EXCEEDED) { validator.validate(TENANT, target, fiveHundredOneNodes) }
    }

    @Test
    fun dependencyGraphRejectsDirectAndIndirectHasTagCycles() {
        val graph = TagRuleDependencyGraph()

        assertCode(TagErrorCode.RULE_DEPENDENCY_CYCLE) {
            graph.requireAcyclic("A", setOf("A"), emptyMap())
        }
        assertCode(TagErrorCode.RULE_DEPENDENCY_CYCLE) {
            graph.requireAcyclic("C", setOf("A"), mapOf("A" to setOf("B"), "B" to setOf("C")))
        }
        graph.requireAcyclic("C", setOf("D"), mapOf("A" to setOf("B"), "B" to setOf("C")))
    }

    private fun prepareCatalog(): TagDefinition {
        catalog.registerSubjectType(RegisterTagSubjectTypeCommand(SUBJECT, "Person", "hr"))
        catalog.registerSubjectType(RegisterTagSubjectTypeCommand(FOREIGN_SUBJECT, "Account", "identity"))
        catalog.createAttribute(CreateTagAttributeCommand(TENANT, SUBJECT, "age", "Age", TagAttributeType.INTEGER, TagAttributeCardinality.SINGLE))
        catalog.createAttribute(CreateTagAttributeCommand(TENANT, SUBJECT, "enabled", "Enabled", TagAttributeType.BOOLEAN, TagAttributeCardinality.SINGLE))
        catalog.createAttribute(CreateTagAttributeCommand(TENANT, SUBJECT, "name", "Name", TagAttributeType.STRING, TagAttributeCardinality.SINGLE))
        catalog.createAttribute(CreateTagAttributeCommand(TENANT, FOREIGN_SUBJECT, "foreign_age", "Foreign age", TagAttributeType.INTEGER, TagAttributeCardinality.SINGLE))
        val target = catalog.createTag(CreateTagCommand(TENANT, SUBJECT, "adult", "Adult", manualAssignable = false))
        catalog.createTag(CreateTagCommand(TENANT, FOREIGN_SUBJECT, "foreign_tag", "Foreign tag", manualAssignable = false))
        return requireNotNull(tagDao.get(target.id))
    }

    private fun predicate(code: String, operator: TagRuleOperator, operand: TagAttributeValue) =
        TagRuleExpression.AttributePredicate(code, operator, listOf(operand))

    private fun node(
        idValue: String,
        parent: String?,
        kind: String,
        position: Int,
        operator: String? = null,
    ) = TagRuleNode().apply {
        id = idValue
        ruleId = "rule"
        parentId = parent
        nodeKind = kind
        orderNum = position
        attributeId = if (kind == "ATTRIBUTE_PREDICATE") "attribute" else null
        referencedTagId = null
        this.operator = operator
    }

    private fun operand(idValue: String, node: String, position: Int) = TagRuleOperand().apply {
        id = idValue
        nodeId = node
        orderNum = position
        valueType = "INTEGER"
        stringValue = null
        integerValue = 1
        decimalValue = null
        booleanValue = null
        dateValue = null
        datetimeValue = null
    }

    private fun assertCode(code: TagErrorCode, block: () -> Unit) {
        assertEquals(code, assertFailsWith<TagValidationException>(block = block).errorCode)
    }

    private companion object {
        const val TENANT = "tenant-a"
        const val SUBJECT = "hr.person"
        const val FOREIGN_SUBJECT = "identity.account"
    }
}
