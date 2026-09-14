package io.kudos.ms.tag.core.rule.validation

import io.kudos.ms.tag.common.attribute.model.TagAttributeCardinality
import io.kudos.ms.tag.common.attribute.model.TagAttributeType
import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.error.TagErrorCode
import io.kudos.ms.tag.common.error.TagValidationException
import io.kudos.ms.tag.common.rule.model.TagRuleExpression
import io.kudos.ms.tag.common.rule.model.TagRuleOperator
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.catalog.attribute.model.po.TagAttributeDefinition
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tag.model.po.TagDefinition
import io.kudos.ms.tag.core.rule.dao.TagRuleDependencyDao
import io.kudos.ms.tag.core.rule.dao.TagRuleDao
import io.kudos.ms.tag.core.rule.model.po.TagRuleNode
import io.kudos.ms.tag.core.rule.model.po.TagRuleOperand
import org.springframework.stereotype.Component

data class ResolvedRuleReferences(
    val attributes: Map<String, TagAttributeDefinition>,
    val tags: Map<String, TagDefinition>,
)

@Component
open class TagRuleValidator(
    private val attributeDao: TagAttributeDefinitionDao,
    private val tagDao: TagDefinitionDao,
    private val ruleDao: TagRuleDao,
    private val dependencyDao: TagRuleDependencyDao,
    private val dependencyGraph: TagRuleDependencyGraph = TagRuleDependencyGraph(),
) {

    open fun validate(
        tenantId: String,
        targetTag: TagDefinition,
        expression: TagRuleExpression,
    ): ResolvedRuleReferences {
        require(targetTag.tenantId == tenantId) { "Target tag does not belong to tenant [$tenantId]." }
        val attributes = linkedMapOf<String, TagAttributeDefinition>()
        val tags = linkedMapOf<String, TagDefinition>()
        var nodeCount = 0

        fun visit(node: TagRuleExpression, depth: Int) {
            nodeCount++
            checkLimit(depth <= MAX_DEPTH, "Rule depth exceeds $MAX_DEPTH.")
            checkLimit(nodeCount <= MAX_NODES, "Rule node count exceeds $MAX_NODES.")
            when (node) {
                is TagRuleExpression.AllOf -> node.children.forEach { visit(it, depth + 1) }
                is TagRuleExpression.AnyOf -> node.children.forEach { visit(it, depth + 1) }
                is TagRuleExpression.Not -> visit(node.child, depth + 1)
                is TagRuleExpression.AttributePredicate -> {
                    val attribute = attributeDao.findByCode(tenantId, targetTag.subjectType, node.attributeCode)
                        ?.takeIf { it.active }
                        ?: fail(TagErrorCode.RULE_REFERENCE_NOT_FOUND) {
                            "Attribute [${node.attributeCode}] is not in tenant [$tenantId] and subject type [${targetTag.subjectType}]."
                        }
                    validatePredicate(attribute, node)
                    attributes[node.attributeCode] = attribute
                }
                is TagRuleExpression.HasTag -> {
                    val referenced = tagDao.findByCode(tenantId, targetTag.subjectType, node.tagCode)
                        ?.takeIf { it.active }
                        ?: fail(TagErrorCode.RULE_REFERENCE_NOT_FOUND) {
                            "Tag [${node.tagCode}] is not in tenant [$tenantId] and subject type [${targetTag.subjectType}]."
                        }
                    tags[node.tagCode] = referenced
                }
            }
        }

        visit(expression, 1)
        dependencyGraph.requireAcyclic(targetTag.id, tags.values.mapTo(linkedSetOf()) { it.id }, currentEdges(tenantId, targetTag.id))
        return ResolvedRuleReferences(attributes, tags)
    }

    open fun validateStructure(
        rootNodeId: String?,
        nodes: List<TagRuleNode>,
        operands: List<TagRuleOperand>,
    ) {
        checkStructure(nodes.isNotEmpty(), "Rule tree is empty.")
        checkLimit(nodes.size <= MAX_NODES, "Rule node count exceeds $MAX_NODES.")
        val byId = nodes.associateBy { it.id }
        checkStructure(byId.size == nodes.size, "Rule node ids must be unique.")
        val roots = nodes.filter { it.parentId == null }
        checkStructure(roots.size == 1 && roots.single().id == rootNodeId, "Rule must have exactly the declared root.")
        checkStructure(nodes.all { it.parentId == null || byId.containsKey(it.parentId) }, "Rule contains an unknown parent.")
        checkStructure(
            nodes.groupBy { it.parentId }.values.all { siblings -> siblings.map { it.orderNum }.distinct().size == siblings.size },
            "Sibling positions must be unique.",
        )
        checkStructure(
            operands.groupBy { it.nodeId }.values.all { values -> values.map { it.orderNum }.distinct().size == values.size },
            "Operand positions must be unique.",
        )

        val children = nodes.groupBy { it.parentId }
        val operandsByNode = operands.groupBy { it.nodeId }
        val visited = mutableSetOf<String>()
        fun visit(node: TagRuleNode, depth: Int) {
            checkStructure(visited.add(node.id), "Rule tree contains a node cycle.")
            checkLimit(depth <= MAX_DEPTH, "Rule depth exceeds $MAX_DEPTH.")
            val childCount = children[node.id].orEmpty().size
            val nodeOperands = operandsByNode[node.id].orEmpty()
            when (node.nodeKind) {
                "ALL_OF", "ANY_OF" -> checkStructure(
                    childCount > 0 && nodeOperands.isEmpty() && node.attributeId == null &&
                        node.referencedTagId == null && node.operator == null,
                    "Rule group has an invalid shape.",
                )
                "NOT" -> checkStructure(
                    childCount == 1 && nodeOperands.isEmpty() && node.attributeId == null &&
                        node.referencedTagId == null && node.operator == null,
                    "NOT must have exactly one child and no predicate fields.",
                )
                "HAS_TAG" -> checkStructure(
                    childCount == 0 && nodeOperands.isEmpty() && node.attributeId == null &&
                        node.referencedTagId != null && node.operator == null,
                    "HasTag must be a leaf with exactly one tag reference.",
                )
                "ATTRIBUTE_PREDICATE" -> {
                    checkStructure(
                        childCount == 0 && node.attributeId != null && node.referencedTagId == null,
                        "Attribute predicate must be a leaf with exactly one attribute reference.",
                    )
                    val operator = runCatching { TagRuleOperator.valueOf(requireNotNull(node.operator)) }.getOrNull()
                        ?: fail(TagErrorCode.RULE_STRUCTURE_INVALID) { "Attribute predicate operator is invalid." }
                    checkOperandCount(operator, nodeOperands.size)
                }
                else -> fail(TagErrorCode.RULE_STRUCTURE_INVALID) { "Unknown rule node kind [${node.nodeKind}]." }
            }
            children[node.id].orEmpty().sortedBy { it.orderNum }.forEach { visit(it, depth + 1) }
        }
        visit(roots.single(), 1)
        checkStructure(visited.size == nodes.size, "Rule contains nodes unreachable from the root.")
        checkStructure(operands.all { it.nodeId in byId }, "Rule contains an operand for an unknown node.")
    }

    private fun validatePredicate(attribute: TagAttributeDefinition, predicate: TagRuleExpression.AttributePredicate) {
        checkOperandCount(predicate.operator, predicate.operands.size)
        val allowed = when (attribute.valueType) {
            TagAttributeType.STRING -> STRING_OPERATORS
            TagAttributeType.INTEGER, TagAttributeType.DECIMAL, TagAttributeType.DATE, TagAttributeType.DATETIME -> ORDERED_OPERATORS
            TagAttributeType.BOOLEAN -> BASIC_OPERATORS
        } + if (attribute.cardinality == TagAttributeCardinality.MULTIPLE) setOf(TagRuleOperator.CONTAINS) else emptySet()
        if (predicate.operator !in allowed) {
            fail(TagErrorCode.RULE_OPERATOR_TYPE_MISMATCH) {
                "Operator [${predicate.operator}] is not valid for ${attribute.valueType}/${attribute.cardinality}."
            }
        }
        if (predicate.operands.any { valueType(it) != attribute.valueType }) {
            fail(TagErrorCode.RULE_OPERAND_TYPE_MISMATCH) {
                "Operands for [${attribute.code}] must all be ${attribute.valueType}."
            }
        }
        if (predicate.operator == TagRuleOperator.BETWEEN && compare(predicate.operands[0], predicate.operands[1]) > 0) {
            fail(TagErrorCode.RULE_OPERAND_ORDER_INVALID) { "BETWEEN lower operand must not exceed its upper operand." }
        }
    }

    private fun currentEdges(tenantId: String, targetTagId: String): Map<String, Set<String>> {
        val currentRules = ruleDao.listNonRetired(tenantId)
            .filter { it.tagId != targetTagId && it.status != "DRAFT" }
            .groupBy { it.tagId }
            .mapValues { (_, versions) -> versions.maxBy { it.ruleVersion } }
        val ruleToTag = currentRules.values.associate { it.id to it.tagId }
        return dependencyDao.listByTenant(tenantId)
            .asSequence()
            .filter { it.dependencyType == "TAG" && it.ruleId in ruleToTag }
            .groupBy({ requireNotNull(ruleToTag[it.ruleId]) }, { requireNotNull(it.referencedTagId) })
            .mapValues { (_, values) -> values.toSet() }
    }

    private fun checkOperandCount(operator: TagRuleOperator, count: Int) {
        val valid = when (operator) {
            TagRuleOperator.EXISTS, TagRuleOperator.NOT_EXISTS -> count == 0
            TagRuleOperator.BETWEEN -> count == 2
            TagRuleOperator.IN, TagRuleOperator.NOT_IN -> count > 0
            else -> count == 1
        }
        if (!valid) fail(TagErrorCode.INVALID_OPERAND_COUNT) { "Operator [$operator] received $count operands." }
    }

    private fun valueType(value: TagAttributeValue) = when (value) {
        is TagAttributeValue.StringValue -> TagAttributeType.STRING
        is TagAttributeValue.IntegerValue -> TagAttributeType.INTEGER
        is TagAttributeValue.DecimalValue -> TagAttributeType.DECIMAL
        is TagAttributeValue.BooleanValue -> TagAttributeType.BOOLEAN
        is TagAttributeValue.DateValue -> TagAttributeType.DATE
        is TagAttributeValue.DateTimeValue -> TagAttributeType.DATETIME
    }

    private fun compare(left: TagAttributeValue, right: TagAttributeValue): Int = when (left) {
        is TagAttributeValue.StringValue -> left.value.compareTo((right as TagAttributeValue.StringValue).value)
        is TagAttributeValue.IntegerValue -> left.value.compareTo((right as TagAttributeValue.IntegerValue).value)
        is TagAttributeValue.DecimalValue -> left.toBigDecimal().compareTo((right as TagAttributeValue.DecimalValue).toBigDecimal())
        is TagAttributeValue.BooleanValue -> left.value.compareTo((right as TagAttributeValue.BooleanValue).value)
        is TagAttributeValue.DateValue -> left.toLocalDate().compareTo((right as TagAttributeValue.DateValue).toLocalDate())
        is TagAttributeValue.DateTimeValue -> left.toInstant().compareTo((right as TagAttributeValue.DateTimeValue).toInstant())
    }

    private fun checkLimit(condition: Boolean, message: String) {
        if (!condition) fail(TagErrorCode.RULE_LIMIT_EXCEEDED) { message }
    }

    private fun checkStructure(condition: Boolean, message: String) {
        if (!condition) fail(TagErrorCode.RULE_STRUCTURE_INVALID) { message }
    }

    private fun fail(code: TagErrorCode, message: () -> String): Nothing = throw TagValidationException(code, message())

    private companion object {
        const val MAX_DEPTH = 20
        const val MAX_NODES = 500
        val BASIC_OPERATORS = setOf(
            TagRuleOperator.EQ, TagRuleOperator.NE, TagRuleOperator.IN, TagRuleOperator.NOT_IN,
            TagRuleOperator.EXISTS, TagRuleOperator.NOT_EXISTS,
        )
        val STRING_OPERATORS = BASIC_OPERATORS + TagRuleOperator.CONTAINS
        val ORDERED_OPERATORS = BASIC_OPERATORS + setOf(
            TagRuleOperator.GT, TagRuleOperator.GTE, TagRuleOperator.LT, TagRuleOperator.LTE, TagRuleOperator.BETWEEN,
        )
    }
}
