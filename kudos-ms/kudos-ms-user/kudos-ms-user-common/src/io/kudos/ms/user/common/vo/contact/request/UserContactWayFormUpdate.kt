package io.kudos.ms.user.common.vo.contact.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 用户联系方式表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserContactWayFormUpdate (

    /** 主键 */
    override val id: String? = null,

    /** 用户ID */
    val userId: String? = null,

    /** 联系方式字典码 */
    val contactWayDictCode: String? = null,

    /** 联系方式值 */
    val contactWayValue: String? = null,

    /** 联系方式状态字典码 */
    val contactWayStatusDictCode: String? = null,

    /** 优先级 */
    val priority: Short? = null,

    /** 备注 */
    val remark: String? = null,

) : IIdEntity<String?>
