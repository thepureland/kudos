package io.kudos.ms.sys.common.vo.accessruleip

import io.kudos.base.support.payload.ListSearchPayload
import java.time.LocalDateTime
import kotlin.reflect.KClass


/**
 * ip访问规则查询条件载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpQuery (


    /** 主键 */
    val id: String? = null,

    /** ip起 */
    val ipStart: Long? = null,

    /** ip止 */
    val ipEnd: Long? = null,

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
    val ruleTypeDictCode: String? = null,

) : ListSearchPayload() {


    constructor() : this("")

    override val returnEntityClass: KClass<*> = SysAccessRuleIpRow::class

}
