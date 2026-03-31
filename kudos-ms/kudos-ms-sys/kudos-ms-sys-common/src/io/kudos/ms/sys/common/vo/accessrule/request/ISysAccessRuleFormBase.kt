package io.kudos.ms.sys.common.vo.accessrule.request

/**
 * 访问规则表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface ISysAccessRuleFormBase {

    /** 租户id */
    val tenantId: String?

    /** 系统编码 */
    val systemCode: String?

    /** 规则类型 */
    val ruleType: Int?
}
