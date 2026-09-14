package io.kudos.ms.tag.core.rule

import io.kudos.ms.tag.core.TagDaoTestSupport
import io.kudos.ms.tag.core.rule.dao.TagRuleDao
import io.kudos.ms.tag.core.rule.dao.TagRuleDependencyDao
import io.kudos.ms.tag.core.rule.dao.TagRuleNodeDao
import io.kudos.ms.tag.core.rule.dao.TagRuleOperandDao
import io.kudos.ms.tag.core.rule.model.po.TagRule
import io.kudos.ms.tag.core.rule.model.po.TagRuleDependency
import io.kudos.ms.tag.core.rule.model.po.TagRuleNode
import io.kudos.ms.tag.core.rule.model.po.TagRuleOperand
import org.ktorm.database.Database
import io.kudos.context.core.KudosContextHolder
import io.kudos.ability.data.rdb.ktorm.datasource.currentDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class TagRuleDaoTest : TagDaoTestSupport() {

    @Test
    fun ruleTreeLoadsInDeterministicOrderAndNeverCrossesTenant() {
        seedCatalog(KudosContextHolder.currentDatabase())
        TagRuleDao().insert(TagRule().apply {
            id = RULE_ID
            tenantId = "tenant-a"
            tagId = TAG_ID
            ruleVersion = 1
            status = "DRAFT"
            expressionVersion = 1
            checksum = "checksum"
            version = 0
        })
        val nodeDao = TagRuleNodeDao()
        nodeDao.insert(node(NODE_B_ID, 2))
        nodeDao.insert(node(NODE_A_ID, 1))
        val operandDao = TagRuleOperandDao()
        operandDao.insert(operand(OPERAND_B_ID, NODE_A_ID, 2, "200"))
        operandDao.insert(operand(OPERAND_A_ID, NODE_A_ID, 1, "100"))
        TagRuleDependencyDao().insert(TagRuleDependency().apply {
            id = DEPENDENCY_ID
            tenantId = "tenant-a"
            ruleId = RULE_ID
            tagId = TAG_ID
            dependencyType = "ATTRIBUTE"
            attributeId = ATTRIBUTE_ID
        })

        val tree = requireNotNull(TagRuleDao().loadTree("tenant-a", RULE_ID, 1))
        assertEquals(listOf(NODE_A_ID, NODE_B_ID), tree.nodes.map { it.id })
        assertEquals(listOf(OPERAND_A_ID, OPERAND_B_ID), tree.operands.map { it.id })
        assertEquals(listOf(DEPENDENCY_ID), tree.dependencies.map { it.id })
        assertNull(TagRuleDao().loadTree("tenant-b", RULE_ID, 1))
    }

    private fun node(idValue: String, order: Int) = TagRuleNode().apply {
        id = idValue
        ruleId = RULE_ID
        nodeKind = "ATTRIBUTE_PREDICATE"
        orderNum = order
        attributeId = ATTRIBUTE_ID
        operator = "GTE"
    }

    private fun operand(idValue: String, nodeIdValue: String, order: Int, value: String) = TagRuleOperand().apply {
        id = idValue
        nodeId = nodeIdValue
        orderNum = order
        valueType = "DECIMAL"
        decimalValue = value.toBigDecimal()
    }

    private fun seedCatalog(database: Database) {
        database.useConnection { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("insert into tag_subject_type (id, code, name, owner_service_code, active, built_in, version, create_time, update_time) values ('00000000-0000-0000-0000-000000000001', 'estate.house', 'House', 'estate', true, false, 0, current_timestamp, current_timestamp)")
                statement.executeUpdate("insert into tag_attribute_definition (id, tenant_id, subject_type, code, name, value_type, cardinality, active, built_in, version, create_time, update_time) values ('$ATTRIBUTE_ID', 'tenant-a', 'estate.house', 'area', 'Area', 'DECIMAL', 'SINGLE', true, false, 0, current_timestamp, current_timestamp)")
                statement.executeUpdate("insert into tag_definition (id, tenant_id, subject_type, code, name, set_priority, manual_assignable, active, built_in, version, create_time, update_time) values ('$TAG_ID', 'tenant-a', 'estate.house', 'large', 'Large', 0, true, true, false, 0, current_timestamp, current_timestamp)")
            }
        }
    }

    private companion object {
        const val RULE_ID = "00000000-0000-0000-0000-000000000401"
        const val TAG_ID = "00000000-0000-0000-0000-000000000004"
        const val ATTRIBUTE_ID = "00000000-0000-0000-0000-000000000002"
        const val NODE_A_ID = "00000000-0000-0000-0000-000000000501"
        const val NODE_B_ID = "00000000-0000-0000-0000-000000000502"
        const val OPERAND_A_ID = "00000000-0000-0000-0000-000000000601"
        const val OPERAND_B_ID = "00000000-0000-0000-0000-000000000602"
        const val DEPENDENCY_ID = "00000000-0000-0000-0000-000000000701"
    }
}
