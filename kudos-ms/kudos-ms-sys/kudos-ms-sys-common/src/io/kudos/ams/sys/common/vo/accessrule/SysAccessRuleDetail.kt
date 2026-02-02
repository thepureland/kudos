package io.kudos.ms.sys.common.vo.accessrule

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 访问规则查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleDetail (

    //region your codes 1

    /** 租户id */
    var tenantId: String? = null,

    /** 系统编码 */
    var systemCode: String? = null,

    /** 规则类型 */
    var ruleType: Int? = null,

    /** 创建者id */
    var createUserId: String? = null,

    /** 创建者名称 */
    var createUserName: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新者id */
    var updateUserId: String? = null,

    /** 更新者名称 */
    var updateUserName: String? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    //endregion your codes 3

}