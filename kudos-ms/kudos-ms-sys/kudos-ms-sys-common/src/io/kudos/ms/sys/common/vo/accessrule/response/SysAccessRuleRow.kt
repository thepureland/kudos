package io.kudos.ms.sys.common.vo.accessrule.response


/**
 * 访问规则列表查询结果响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleRow (

    /** 主键 */
    val id: String = "",

    /** 租户id */
    val tenantId: String? = null,

    /** 系统编码 */
    val systemCode: String? = null,

    /** 规则类型 */
    val ruleType: Int? = null,

)