package io.kudos.ms.sys.common.vo.accessrule.request

/**
 * 访问规则表单基础字段（新建 / 更新共用）。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysAccessRuleFormBase {

    /** 租户id */
    val tenantId: String?

    /** 系统编码 */
    val systemCode: String?

    /** 访问规则类型字典代码（列 `access_rule_type_dict_code`） */
    val accessRuleTypeDictCode: String?

    /** 备注 */
    val remark: String?
}
