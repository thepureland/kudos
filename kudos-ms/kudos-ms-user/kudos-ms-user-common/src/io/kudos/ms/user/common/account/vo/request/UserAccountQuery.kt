package io.kudos.ms.user.common.account.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.user.common.account.vo.response.UserAccountRow


/**
 * 用户列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountQuery (

    /** 用户名 */
    val username: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 用户类型字典码 */
    val accountTypeDictCode: String? = null,

    /** 用户状态字典码 */
    val accountStatusDictCode: String? = null,

    /** 机构id */
    val orgId: String? = null,

    /** 主管id */
    val supervisorId: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否激活 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = UserAccountRow::class

}