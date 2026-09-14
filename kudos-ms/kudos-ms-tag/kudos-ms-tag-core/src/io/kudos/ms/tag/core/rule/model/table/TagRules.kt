package io.kudos.ms.tag.core.rule.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.tag.core.rule.model.po.TagRule
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object TagRules : StringIdTable<TagRule>("tag_rule") {
    val tenantId = varchar("tenant_id").bindTo { it.tenantId }
    val tagId = varchar("tag_id").bindTo { it.tagId }
    val ruleVersion = long("rule_version").bindTo { it.ruleVersion }
    val status = varchar("status").bindTo { it.status }
    val rootNodeId = varchar("root_node_id").bindTo { it.rootNodeId }
    val expressionVersion = int("expression_version").bindTo { it.expressionVersion }
    val checksum = varchar("checksum").bindTo { it.checksum }
    val publishedTime = datetime("published_time").bindTo { it.publishedTime }
    val retiredTime = datetime("retired_time").bindTo { it.retiredTime }
    val version = long("version").bindTo { it.version }
    val createTime = datetime("create_time").bindTo { it.createTime }
    val createUserId = varchar("create_user_id").bindTo { it.createUserId }
    val createUserName = varchar("create_user_name").bindTo { it.createUserName }
    val updateTime = datetime("update_time").bindTo { it.updateTime }
    val updateUserId = varchar("update_user_id").bindTo { it.updateUserId }
    val updateUserName = varchar("update_user_name").bindTo { it.updateUserName }
}
