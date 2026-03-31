package io.kudos.ms.user.common.vo.org.request


/**
 * 机构表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserOrgFormCreate (

    override val name: String? = null,

    override val shortName: String? = null,

    override val tenantId: String? = null,

    override val parentId: String? = null,

    override val orgTypeDictCode: String? = null,

    override val sortNum: Int? = null,

    override val remark: String? = null,

) : IUserOrgFormBase
