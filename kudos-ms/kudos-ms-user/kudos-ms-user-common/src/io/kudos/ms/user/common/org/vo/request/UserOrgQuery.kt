package io.kudos.ms.user.common.org.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.user.common.org.vo.response.UserOrgRow


/**
 * 机构列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserOrgQuery (

    /** 机构名称 */
    val name: String? = null,

    /** 机构简称 */
    val shortName: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 父机构id */
    val parentId: String? = null,

    /** 机构类型字典码 */
    val orgTypeDictCode: String? = null,

    /** 是否激活 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = UserOrgRow::class

}
