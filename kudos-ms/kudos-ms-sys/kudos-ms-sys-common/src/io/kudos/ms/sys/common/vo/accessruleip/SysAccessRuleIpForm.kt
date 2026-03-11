package io.kudos.ms.sys.common.vo.accessruleip

import io.kudos.base.support.payload.FormPayload
import java.time.LocalDateTime


/**
 * ip访问规则表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpForm (

    //region your codes 1

    override val id: String? = null,

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

    //endregion your codes 1
//region your codes 2
) : FormPayload<String?>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}