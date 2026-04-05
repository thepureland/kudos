package io.kudos.ms.sys.common.accessrule.vo.request
import java.time.LocalDateTime


/**
 * IP访问规则表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpFormCreate (

    override val ipStart: Long? ,

    override val ipEnd: Long? ,

    override val ipType: Int? ,

    override val expirationDate: LocalDateTime? ,

    override val parentRuleId: String? ,

    override val remark: String? ,

    override val active: Boolean? ,

) : ISysAccessRuleIpFormBase
