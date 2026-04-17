package io.kudos.ms.user.common.contact.vo.request

/**
 * 用户联系方式表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserContactWayFormCreate (

    override val userId: String? ,

    override val contactWayDictCode: String? ,

    override val contactWayValue: String? ,

    override val contactWayStatusDictCode: String? ,

    override val priority: Short? ,

    override val remark: String? ,

) : IUserContactWayFormBase
