package io.kudos.ms.sys.common.vo.accessrule

import io.kudos.base.support.payload.FormPayload


/**
 * 访问规则表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleForm (


    override val id: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    /** 规则类型 */
    val ruleType: Int? = null,

) : FormPayload<String?>() {


    constructor() : this(null)


}