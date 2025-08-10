package io.kudos.ams.sys.common.vo.accessruleip

import io.kudos.base.support.payload.FormPayload
import java.time.LocalDateTime


/**
 * ip访问规则表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpPayload (

    //region your codes 1

    /** ip起 */
    var ipStart: Long? = null,

    /** ip止 */
    var ipEnd: Long? = null,

    /** ip类型 */
    var ipType: Int? = null,

    /** 过期时间 */
    var expirationDate: LocalDateTime? = null,

    /** 父规则id */
    var parentRuleId: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}