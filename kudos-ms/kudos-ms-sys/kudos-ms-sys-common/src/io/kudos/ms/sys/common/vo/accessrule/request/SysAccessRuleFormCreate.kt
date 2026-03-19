package io.kudos.ms.sys.common.vo.accessrule.request


/**
 * 访问规则表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleFormCreate (

    /** 租户id */
    val tenantId: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    /** 规则类型 */
    val ruleType: Int? = null,

)