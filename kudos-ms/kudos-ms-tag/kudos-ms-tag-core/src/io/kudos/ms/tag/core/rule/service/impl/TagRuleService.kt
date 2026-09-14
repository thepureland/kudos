package io.kudos.ms.tag.core.rule.service.impl

import io.kudos.ms.tag.common.attribute.model.TagAttributeValue
import io.kudos.ms.tag.common.rule.model.TagRuleExpression
import io.kudos.ms.tag.common.rule.model.TagRuleOperator
import io.kudos.ms.tag.core.catalog.tag.dao.TagDefinitionDao
import io.kudos.ms.tag.core.catalog.tag.model.po.TagDefinition
import io.kudos.ms.tag.core.catalog.attribute.dao.TagAttributeDefinitionDao
import io.kudos.ms.tag.core.rule.dao.TagRuleDao
import io.kudos.ms.tag.core.rule.dao.TagRuleDependencyDao
import io.kudos.ms.tag.core.rule.dao.TagRuleNodeDao
import io.kudos.ms.tag.core.rule.dao.TagRuleOperandDao
import io.kudos.ms.tag.core.rule.model.PersistedRuleTree
import io.kudos.ms.tag.core.rule.model.TagRulePublication
import io.kudos.ms.tag.core.rule.model.TagRuleStatus
import io.kudos.ms.tag.core.rule.model.TagRuleView
import io.kudos.ms.tag.core.rule.model.po.TagRule
import io.kudos.ms.tag.core.rule.model.po.TagRuleDependency
import io.kudos.ms.tag.core.rule.model.po.TagRuleNode
import io.kudos.ms.tag.core.rule.model.po.TagRuleOperand
import io.kudos.ms.tag.core.rule.service.iservice.ITagRuleService
import io.kudos.ms.tag.core.rule.validation.ResolvedRuleReferences
import io.kudos.ms.tag.core.rule.validation.TagRuleValidator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
@Transactional
open class TagRuleService(
    private val tagDao: TagDefinitionDao,
    private val attributeDao: TagAttributeDefinitionDao,
    private val ruleDao: TagRuleDao,
    private val nodeDao: TagRuleNodeDao,
    private val operandDao: TagRuleOperandDao,
    private val dependencyDao: TagRuleDependencyDao,
    private val validator: TagRuleValidator,
) : ITagRuleService {

    override fun createDraft(tenantId: String, tagCode: String, expression: TagRuleExpression): TagRuleView {
        val tag = requireNotNull(findTag(tenantId, tagCode)) { "Tag [$tagCode] does not exist for tenant [$tenantId]." }
        require(ruleDao.listByTag(tenantId, tag.id).none { it.status == TagRuleStatus.DRAFT.name }) {
            "Tag [$tagCode] already has a draft rule."
        }
        val references = validator.validate(tenantId, tag, expression)
        val rule = TagRule().apply {
            id = newId()
            this.tenantId = tenantId
            tagId = tag.id
            ruleVersion = (ruleDao.listByTag(tenantId, tag.id).maxOfOrNull { it.ruleVersion } ?: 0) + 1
            status = TagRuleStatus.DRAFT.name
            rootNodeId = null
            expressionVersion = EXPRESSION_VERSION
            checksum = DRAFT_CHECKSUM
            publishedTime = null
            retiredTime = null
            version = 0
        }
        ruleDao.insert(rule)
        persistTree(rule, expression, references)
        return rule.toView(tag, expression)
    }

    override fun replaceDraft(tenantId: String, ruleId: String, expression: TagRuleExpression): TagRuleView {
        val rule = requireRule(tenantId, ruleId)
        require(rule.status == TagRuleStatus.DRAFT.name) { "Only a DRAFT rule may be replaced." }
        val tag = requireNotNull(tagDao.get(rule.tagId)) { "Tag [${rule.tagId}] does not exist." }
        val references = validator.validate(tenantId, tag, expression)
        removeTree(rule)
        rule.checksum = DRAFT_CHECKSUM
        rule.version += 1
        persistTree(rule, expression, references)
        return rule.toView(tag, expression)
    }

    override fun requestPublication(tenantId: String, ruleId: String): TagRulePublication {
        val rule = requireRule(tenantId, ruleId)
        require(rule.status == TagRuleStatus.DRAFT.name) { "Only a DRAFT rule may be submitted for publication." }
        val tag = requireNotNull(tagDao.get(rule.tagId)) { "Tag [${rule.tagId}] does not exist." }
        val tree = requireNotNull(ruleDao.loadTree(tenantId, rule.id, rule.ruleVersion)) { "Rule tree [$ruleId] does not exist." }
        validator.validateStructure(rule.rootNodeId, tree.nodes, tree.operands)
        val expression = restoreExpression(tree)
        validator.validate(tenantId, tag, expression)

        rule.status = TagRuleStatus.VALIDATING.name
        rule.checksum = checksum(expression)
        rule.version += 1
        check(ruleDao.update(rule)) { "Rule [$ruleId] could not enter validation." }
        rule.status = TagRuleStatus.REBUILDING.name
        rule.version += 1
        check(ruleDao.update(rule)) { "Rule [$ruleId] could not enter rebuild." }
        return TagRulePublication(rule.toView(tag, expression), "rule-full-rebuild:${rule.id}:${rule.ruleVersion}")
    }

    /** Called by the successful full-rebuild promotion transaction implemented in Task 14. */
    internal fun completePublication(tenantId: String, ruleId: String): TagRuleView {
        val rule = requireRule(tenantId, ruleId)
        require(rule.status == TagRuleStatus.REBUILDING.name) { "Only a REBUILDING rule may be promoted." }
        val tag = requireNotNull(tagDao.get(rule.tagId)) { "Tag [${rule.tagId}] does not exist." }
        val now = LocalDateTime.now(ZoneOffset.UTC)
        ruleDao.listByTag(tenantId, tag.id)
            .filter { it.id != rule.id && it.status == TagRuleStatus.PUBLISHED.name }
            .forEach {
                it.status = TagRuleStatus.RETIRED.name
                it.retiredTime = now
                it.version += 1
                check(ruleDao.update(it)) { "Previous rule [${it.id}] could not be retired." }
            }
        rule.status = TagRuleStatus.PUBLISHED.name
        rule.publishedTime = now
        rule.version += 1
        check(ruleDao.update(rule)) { "Rule [$ruleId] could not be published." }
        tag.publishedRuleId = rule.id
        tag.version += 1
        check(tagDao.updateCatalog(tag)) { "Tag [${tag.id}] could not switch its published rule." }
        return rule.toView(tag, restoreExpression(requireNotNull(ruleDao.loadTree(tenantId, rule.id, rule.ruleVersion))))
    }

    @Transactional(readOnly = true)
    override fun getPublished(tenantId: String, tagCode: String): TagRuleView? {
        val tag = findTag(tenantId, tagCode) ?: return null
        val ruleId = tag.publishedRuleId ?: return null
        val rule = ruleDao.findByTenantAndId(tenantId, ruleId) ?: return null
        if (rule.status != TagRuleStatus.PUBLISHED.name) return null
        val tree = ruleDao.loadTree(tenantId, rule.id, rule.ruleVersion) ?: return null
        return rule.toView(tag, restoreExpression(tree))
    }

    private fun persistTree(
        rule: TagRule,
        expression: TagRuleExpression,
        references: ResolvedRuleReferences,
    ) {
        fun persistNode(node: TagRuleExpression, parentId: String?, position: Int): String {
            val nodeId = newId()
            val row = TagRuleNode().apply {
                id = nodeId
                ruleId = rule.id
                this.parentId = parentId
                orderNum = position
                nodeKind = when (node) {
                    is TagRuleExpression.AllOf -> "ALL_OF"
                    is TagRuleExpression.AnyOf -> "ANY_OF"
                    is TagRuleExpression.Not -> "NOT"
                    is TagRuleExpression.AttributePredicate -> "ATTRIBUTE_PREDICATE"
                    is TagRuleExpression.HasTag -> "HAS_TAG"
                }
                attributeId = (node as? TagRuleExpression.AttributePredicate)?.let {
                    requireNotNull(references.attributes[it.attributeCode]).id
                }
                referencedTagId = (node as? TagRuleExpression.HasTag)?.let {
                    requireNotNull(references.tags[it.tagCode]).id
                }
                operator = (node as? TagRuleExpression.AttributePredicate)?.operator?.name
            }
            nodeDao.insert(row)
            if (node is TagRuleExpression.AttributePredicate) {
                node.operands.forEachIndexed { operandPosition, value ->
                    operandDao.insert(value.toRow(nodeId, operandPosition))
                }
            }
            when (node) {
                is TagRuleExpression.AllOf -> node.children.forEachIndexed { index, child -> persistNode(child, nodeId, index) }
                is TagRuleExpression.AnyOf -> node.children.forEachIndexed { index, child -> persistNode(child, nodeId, index) }
                is TagRuleExpression.Not -> persistNode(node.child, nodeId, 0)
                is TagRuleExpression.AttributePredicate, is TagRuleExpression.HasTag -> Unit
            }
            return nodeId
        }

        val rootId = persistNode(expression, null, 0)
        references.attributes.values.distinctBy { it.id }.forEach { attribute ->
            dependencyDao.insert(TagRuleDependency().apply {
                id = newId()
                tenantId = rule.tenantId
                ruleId = rule.id
                tagId = rule.tagId
                dependencyType = "ATTRIBUTE"
                attributeId = attribute.id
                referencedTagId = null
            })
        }
        references.tags.values.distinctBy { it.id }.forEach { referenced ->
            dependencyDao.insert(TagRuleDependency().apply {
                id = newId()
                tenantId = rule.tenantId
                ruleId = rule.id
                tagId = rule.tagId
                dependencyType = "TAG"
                attributeId = null
                referencedTagId = referenced.id
            })
        }
        rule.rootNodeId = rootId
        check(ruleDao.update(rule)) { "Rule [${rule.id}] root node could not be recorded." }
    }

    private fun removeTree(rule: TagRule) {
        val tree = requireNotNull(ruleDao.loadTree(rule.tenantId, rule.id, rule.ruleVersion)) { "Rule tree [${rule.id}] does not exist." }
        rule.rootNodeId = null
        check(ruleDao.update(rule)) { "Rule [${rule.id}] root node could not be cleared." }
        tree.operands.forEach { check(operandDao.deleteById(it.id)) { "Operand [${it.id}] could not be deleted." } }
        tree.dependencies.forEach { check(dependencyDao.deleteById(it.id)) { "Dependency [${it.id}] could not be deleted." } }
        val depth = mutableMapOf<String, Int>()
        fun nodeDepth(node: TagRuleNode): Int = depth.getOrPut(node.id) {
            node.parentId?.let { parent -> 1 + nodeDepth(requireNotNull(tree.nodes.find { it.id == parent })) } ?: 0
        }
        tree.nodes.sortedByDescending(::nodeDepth).forEach {
            check(nodeDao.deleteById(it.id)) { "Rule node [${it.id}] could not be deleted." }
        }
    }

    private fun restoreExpression(tree: PersistedRuleTree): TagRuleExpression {
        val byParent = tree.nodes.groupBy { it.parentId }
        val operands = tree.operands.groupBy { it.nodeId }
        fun restore(node: TagRuleNode): TagRuleExpression {
            val children = byParent[node.id].orEmpty().sortedBy { it.orderNum }.map(::restore)
            return when (node.nodeKind) {
                "ALL_OF" -> TagRuleExpression.AllOf(children)
                "ANY_OF" -> TagRuleExpression.AnyOf(children)
                "NOT" -> TagRuleExpression.Not(children.single())
                "HAS_TAG" -> TagRuleExpression.HasTag(requireNotNull(tagDao.get(requireNotNull(node.referencedTagId))).code)
                "ATTRIBUTE_PREDICATE" -> TagRuleExpression.AttributePredicate(
                    requireNotNull(attributeDao.get(requireNotNull(node.attributeId))).code,
                    TagRuleOperator.valueOf(requireNotNull(node.operator)),
                    operands[node.id].orEmpty().sortedBy { it.orderNum }.map { it.toValue() },
                )
                else -> error("Unknown rule node kind [${node.nodeKind}].")
            }
        }
        return restore(requireNotNull(tree.nodes.singleOrNull { it.id == tree.rule.rootNodeId }))
    }

    private fun TagAttributeValue.toRow(ownerNodeId: String, position: Int) = TagRuleOperand().also { row ->
        row.id = newId()
        row.nodeId = ownerNodeId
        row.orderNum = position
        row.valueType = when (this) {
            is TagAttributeValue.StringValue -> "STRING"
            is TagAttributeValue.IntegerValue -> "INTEGER"
            is TagAttributeValue.DecimalValue -> "DECIMAL"
            is TagAttributeValue.BooleanValue -> "BOOLEAN"
            is TagAttributeValue.DateValue -> "DATE"
            is TagAttributeValue.DateTimeValue -> "DATETIME"
        }
        row.stringValue = (this as? TagAttributeValue.StringValue)?.value
        row.integerValue = (this as? TagAttributeValue.IntegerValue)?.value
        row.decimalValue = (this as? TagAttributeValue.DecimalValue)?.toBigDecimal()
        row.booleanValue = (this as? TagAttributeValue.BooleanValue)?.value
        row.dateValue = (this as? TagAttributeValue.DateValue)?.toLocalDate()
        row.datetimeValue = (this as? TagAttributeValue.DateTimeValue)?.toInstant()?.atOffset(ZoneOffset.UTC)?.toLocalDateTime()
    }

    private fun TagRuleOperand.toValue(): TagAttributeValue = when (valueType) {
        "STRING" -> TagAttributeValue.StringValue(requireNotNull(stringValue))
        "INTEGER" -> TagAttributeValue.IntegerValue(requireNotNull(integerValue))
        "DECIMAL" -> TagAttributeValue.DecimalValue(requireNotNull(decimalValue).toPlainString())
        "BOOLEAN" -> TagAttributeValue.BooleanValue(requireNotNull(booleanValue))
        "DATE" -> TagAttributeValue.DateValue(requireNotNull(dateValue).toString())
        "DATETIME" -> TagAttributeValue.DateTimeValue(requireNotNull(datetimeValue).atOffset(ZoneOffset.UTC).toString())
        else -> error("Unknown operand type [$valueType].")
    }

    private fun findTag(tenantId: String, tagCode: String): TagDefinition? {
        val matches = tagDao.listByTenantAndCode(tenantId, tagCode)
        require(matches.size <= 1) {
            "Tag code [$tagCode] is ambiguous across subject types; use a tenant-unique tag code."
        }
        return matches.singleOrNull()
    }

    private fun requireRule(tenantId: String, ruleId: String): TagRule =
        requireNotNull(ruleDao.findByTenantAndId(tenantId, ruleId)) { "Rule [$ruleId] does not exist for tenant [$tenantId]." }

    private fun checksum(expression: TagRuleExpression): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(JSON.encodeToString(expression).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun TagRule.toView(tag: TagDefinition, expression: TagRuleExpression) = TagRuleView(
        id, tenantId, tagId, tag.code, tag.subjectType, ruleVersion, TagRuleStatus.valueOf(status), expression, checksum,
    )

    private fun newId(): String = UUID.randomUUID().toString()

    private companion object {
        const val EXPRESSION_VERSION = 1
        val DRAFT_CHECKSUM = "0".repeat(64)
        val JSON = Json { classDiscriminator = "type"; encodeDefaults = true; explicitNulls = true }
    }
}
