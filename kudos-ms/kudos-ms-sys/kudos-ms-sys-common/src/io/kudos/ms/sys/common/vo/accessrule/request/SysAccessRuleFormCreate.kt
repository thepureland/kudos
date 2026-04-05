package io.kudos.ms.sys.common.vo.accessrule.request


/**
 * 访问规则表单新建请求 VO。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class SysAccessRuleFormCreate (

    override val tenantId: String? ,

    override val systemCode: String? ,

    override val accessRuleTypeDictCode: String?,

    override val remark: String?,

) : ISysAccessRuleFormBase
