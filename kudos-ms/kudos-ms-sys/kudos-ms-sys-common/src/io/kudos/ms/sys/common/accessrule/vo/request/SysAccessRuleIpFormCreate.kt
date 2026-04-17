package io.kudos.ms.sys.common.accessrule.vo.request

import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.ms.sys.common.accessrule.enums.IpTypeEnum
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime


/**
 * IP访问规则表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpFormCreate (

    override val ipv4StartStr: String?,

    override val ipv4EndStr: String?,

    override val ipv6StartStr: String?,

    override val ipv6EndStr: String?,

    override val ipTypeDictCode: String,

    override val expirationDate: LocalDateTime?,

    /** 系统编码 */
    @get:NotBlank
    @get:MaxLength(32)
    val systemCode: String,

    /** 租户id */
    @get:NotBlank
    @get:FixedLength(36)
    val tenantId: String,

    override val remark: String?,

    ) : ISysAccessRuleIpFormBase, IIpStringToBigDecimalSupport {

    override fun getIpStartString(): String {
        return if (IpTypeEnum.IPV4.code == ipTypeDictCode) ipv4StartStr!! else ipv6StartStr!!
    }

    override fun getIpEndString(): String {
        return if (IpTypeEnum.IPV4.code == ipTypeDictCode) ipv4EndStr!! else ipv6EndStr!!
    }

    override fun getIpTypeDictCodeString(): String {
        return ipTypeDictCode
    }

}
