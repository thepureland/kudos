package io.kudos.ms.sys.common.vo.accessruleip.request

import java.time.LocalDateTime


/**
 * IP访问规则表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpFormCreate (

    /** ip起 */
    val ipStart: Long? = null,

    /** ip止 */
    val ipEnd: Long? = null,

    /** ip类型 */
    val ipType: Int? = null,

    /** 过期时间 */
    val expirationDate: LocalDateTime? = null,

    /** 父规则id */
    val parentRuleId: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

)