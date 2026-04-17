package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.sys.common.accessrule.vo.response.SysAccessRuleIpRow
import java.time.LocalDateTime

/**
 * IP访问规则列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpQuery(

    /** 主键 */
    val id: String? = null,

    /** ip 起（字符串表示） */
    val ipStartStr: String? = null,

    /** ip 止（字符串表示）*/
    val ipEndStr: String? = null,

    /** ip类型字典代码 */
    val ipTypeDictCode: String? = null,

    /** 过期时间 */
    val expirationDate: LocalDateTime? = null,

    /** 父规则id */
    val parentRuleId: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 父规则是否启用 */
    val parentRuleActive: Boolean? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    /** 规则类型字典代码 */
    val accessRuleTypeDictCode: String? = null,

    /**
     * 值为 null 但仍需作为查询条件的属性名（例如配合 [tenantId] 为 null 时表示父规则租户 IS NULL）。
     * 语义同 [io.kudos.base.model.payload.ISearchPayload.getNullProperties]。
     */
    val explicitNullProperties: List<String>? = null,

) : ListSearchPayload(), IIpStringToBigDecimalSupport {

    override fun getReturnEntityClass() = SysAccessRuleIpRow::class

    override fun isUnpagedSearchAllowed(): Boolean = true

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