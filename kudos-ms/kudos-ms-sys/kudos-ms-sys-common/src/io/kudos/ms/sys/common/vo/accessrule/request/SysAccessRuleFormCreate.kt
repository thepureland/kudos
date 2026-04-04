package io.kudos.ms.sys.common.vo.accessrule.request


/**
 * 访问规则表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleFormCreate (

    override val tenantId: String? ,

    override val systemCode: String? ,

    override val ruleType: Int? ,

) : ISysAccessRuleFormBase
