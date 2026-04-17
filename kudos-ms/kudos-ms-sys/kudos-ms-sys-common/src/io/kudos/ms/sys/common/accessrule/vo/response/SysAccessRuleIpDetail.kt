package io.kudos.ms.sys.common.accessrule.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.math.BigDecimal
import java.time.LocalDateTime


/**
 * IP 访问规则详情响应 VO；字段语义与 [SysAccessRuleIpEdit] 一致。
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpDetail (

    override val id: String = "",

    val ipStart: BigDecimal? = null,

    val ipEnd: BigDecimal? = null,

    /** ip 类型字典代码 */
    val ipTypeDictCode: String? = null,

    /** 过期时间 */
    val expirationDate: LocalDateTime? = null,

    /** 父规则id */
    val parentRuleId: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 创建者id */
    val createUserId: String? = null,

    /** 创建者名称 */
    val createUserName: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新者id */
    val updateUserId: String? = null,

    /** 更新者名称 */
    val updateUserName: String? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>, IIpBigDecimalToStringSupport {

    override fun getIpStartBigDecimal(): BigDecimal? {
        return ipStart
    }

    override fun getIpEndBigDecimal(): BigDecimal? {
        return ipEnd
    }

    override fun getIpTypeDictCodeStr(): String? {
        return ipTypeDictCode
    }

}