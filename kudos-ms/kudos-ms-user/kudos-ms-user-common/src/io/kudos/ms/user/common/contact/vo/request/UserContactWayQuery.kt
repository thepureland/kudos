package io.kudos.ms.user.common.contact.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.user.common.contact.vo.response.UserContactWayRow


/**
 * 用户联系方式列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserContactWayQuery (

    /** 用户ID */
    val userId: String? = null,

    /** 联系方式字典码 */
    val contactWayDictCode: String? = null,

    /** 联系方式值 */
    val contactWayValue: String? = null,

    /** 联系方式状态字典码 */
    val contactWayStatusDictCode: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = UserContactWayRow::class

}