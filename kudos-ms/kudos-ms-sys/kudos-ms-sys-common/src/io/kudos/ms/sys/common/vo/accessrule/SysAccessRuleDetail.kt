package io.kudos.ms.sys.common.vo.accessrule

import io.kudos.base.model.contract.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 访问规则查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleDetail (


    override val id: String = "",

    /** 租户id */
    val tenantId: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    /** 规则类型 */
    val ruleType: Int? = null,

    /** 创建者id */
    val createUserId: String? = null,

    /** 创建者名称 */
    val createUserName: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新者id */
    val updateUserId: String? = null,

    /** 更新者名称 */
    val updateUserName: String? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

) : IdJsonResult<String>() {


    constructor() : this("")


}