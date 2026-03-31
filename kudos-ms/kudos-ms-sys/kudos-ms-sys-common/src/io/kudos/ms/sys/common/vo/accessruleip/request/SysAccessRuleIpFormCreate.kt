package io.kudos.ms.sys.common.vo.accessruleip.request

import java.time.LocalDateTime


/**
 * IP访问规则表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpFormCreate (

    override val ipStart: Long? = null,

    override val ipEnd: Long? = null,

    override val ipType: Int? = null,

    override val expirationDate: LocalDateTime? = null,

    override val parentRuleId: String? = null,

    override val remark: String? = null,

    override val active: Boolean? = null,

) : ISysAccessRuleIpFormBase
