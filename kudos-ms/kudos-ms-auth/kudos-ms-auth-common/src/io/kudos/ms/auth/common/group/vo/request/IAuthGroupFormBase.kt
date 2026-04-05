package io.kudos.ms.auth.common.group.vo.request
import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/**
 * 用户组表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface IAuthGroupFormBase {

    /** 用户组编码 */
    val code: String?

    /** 用户组名称 */
    val name: String?

    /** 租户id */
    val tenantId: String?

    /** 子系统编码 */
    val subsysCode: String?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
