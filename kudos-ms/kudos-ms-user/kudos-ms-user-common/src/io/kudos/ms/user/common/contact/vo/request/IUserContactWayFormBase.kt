package io.kudos.ms.user.common.contact.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/**
 * 用户联系方式表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface IUserContactWayFormBase {

    /** 用户ID */
    val userId: String?

    /** 联系方式字典码 */
    val contactWayDictCode: String?

    /** 联系方式值 */
    val contactWayValue: String?

    /** 联系方式状态字典码 */
    val contactWayStatusDictCode: String?

    /** 优先级 */
    val priority: Short?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
