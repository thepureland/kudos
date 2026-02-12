package io.kudos.ms.sys.common.vo.accessruleip

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * ip访问规则查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpRecord (

    //region your codes 1

    /** ipRule的id */
    override var id: String = "",

    /** ip起 */
    var ipStart: Long? = null,

    /** ip止 */
    var ipEnd: Long? = null,

    /** ip类型字典代码 */
    var ipTypeDictCode: String? = null,

    /** 过期时间 */
    var expirationTime: LocalDateTime? = null,

    /** 父规则id */
    var parentRuleId: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 父规则是否启用 */
    var parentRuleActive: Boolean? = null,

    /** 租户id */
    var tenantId: String? = null,

    /** 系统编码 */
    var systemCode: String? = null,

    /** 规则类型字典代码 */
    var ruleTypeDictCode: String? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}
