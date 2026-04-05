package io.kudos.ms.user.common.org.vo.request
/**
 * 机构表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserOrgFormCreate (

    override val name: String? ,

    override val shortName: String? ,

    override val tenantId: String? ,

    override val parentId: String? ,

    override val orgTypeDictCode: String? ,

    override val sortNum: Int? ,

    override val remark: String? ,

) : IUserOrgFormBase
