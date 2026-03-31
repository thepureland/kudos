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

    override val id: String? = null,

    override val ipStart: Long? = null,

    override val ipEnd: Long? = null,

    override val ipType: Int? = null,

    override val expirationDate: LocalDateTime? = null,

    override val parentRuleId: String? = null,

    override val remark: String? = null,

    override val active: Boolean? = null,

) : IIdEntity<String?>, ISysAccessRuleIpFormBase
