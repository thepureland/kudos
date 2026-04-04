package io.kudos.ms.sys.common.vo.accessruleip.request

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * IP访问规则表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpFormUpdate (

    override val id: String,

    override val ipStart: Long?,

    override val ipEnd: Long?,

    override val ipType: Int?,

    override val expirationDate: LocalDateTime?,

    override val parentRuleId: String?,

    override val remark: String?,

    override val active: Boolean?,

) : IIdEntity<String>, ISysAccessRuleIpFormBase
