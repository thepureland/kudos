package io.kudos.ms.auth.common.vo.role.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/**
 * 角色表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface IAuthRoleFormBase {

    /** 角色编码 */
    val code: String?

    /** 角色名称 */
    val name: String?

    /** 租户id */
    val tenantId: String?

    /** 子系统编码 */
    val subsysCode: String?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
