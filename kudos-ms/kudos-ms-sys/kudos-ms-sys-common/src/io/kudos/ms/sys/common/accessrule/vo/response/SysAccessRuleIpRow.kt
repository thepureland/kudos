package io.kudos.ms.sys.common.accessrule.vo.response

import java.math.BigDecimal
import java.time.LocalDateTime


/**
 * IP访问规则列表查询结果响应 VO。
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpRow (

    /** ipRule的id */
    val id: String = "",

    /** 区间起点数值 */
    val ipStart: BigDecimal,

    /** 区间终点数值。 */
    val ipEnd: BigDecimal,

    /** ip类型字典代码 */
    val ipTypeDictCode: String,

    /** 过期时间 */
    val expirationTime: LocalDateTime? = null,

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

): IIpBigDecimalToStringSupport {

    override fun getIpStartBigDecimal(): BigDecimal {
        return this.ipStart
    }

    override fun getIpEndBigDecimal(): BigDecimal {
        return this.ipEnd
    }

    override fun getIpTypeDictCodeStr(): String {
        return this.ipTypeDictCode
    }

}
