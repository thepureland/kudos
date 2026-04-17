package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.accessrule.vo.response.VSysAccessRuleWithIpRow

/**
 * 列表/分页查询条件载体，对应视图 `v_sys_access_rule_with_ip`，返回行类型为 [VSysAccessRuleWithIpRow]。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class VSysAccessRuleWithIpQuery(

    /** 视图行主键，语义为 `COALESCE(ip.id, ar.id)` */
    val id: String? = null,

    /** 父访问规则主键 `sys_access_rule.id` */
    val parentId: String? = null,

    /** 父规则租户 id；配合 [explicitNullProperties] 可显式查询 NULL */
    val tenantId: String? = null,

    /** 父规则系统编码 */
    val systemCode: String? = null,

    /** 父规则规则类型字典码 */
    val accessRuleTypeDictCode: String? = null,

    /** 父规则是否启用 */
    val parentActive: Boolean? = null,

    /** 父规则是否内置 */
    val parentBuiltIn: Boolean? = null,

    /** IP 子表主键 `sys_access_rule_ip.id` */
    val ipId: String? = null,

    /** ip 起（字符串表示） */
    val ipStartStr: String? = null,

    /** ip 止（字符串表示）*/
    val ipEndStr: String? = null,

    /** IP 类型字典码 */
    val ipTypeDictCode: String? = null,

    /**
     * 需要按「属性为 NULL」参与 WHERE 的字段名（如仅含 `tenantId` 时表示父规则 `tenant_id IS NULL`）。
     */
    val explicitNullProperties: List<String>? = null,

) : ListSearchPayload(), IIpStringToBigDecimalSupport {

    override fun getReturnEntityClass() = VSysAccessRuleWithIpRow::class

    override fun isUnpagedSearchAllowed(): Boolean = true

    override fun getNullProperties(): List<String>? {
        val raw = explicitNullProperties ?: return null
        if (raw.isEmpty()) return raw
        return raw.map { name ->
            when (name) {
                "ipStartStr" -> "ipStart"
                "ipEndStr" -> "ipEnd"
                else -> name
            }
        }
    }

    override fun getIpStartString(): String? {
        return ipStartStr
    }

    override fun getIpEndString(): String? {
        return ipEndStr
    }

    override fun getIpTypeDictCodeString(): String? {
        return ipTypeDictCode
    }

}
