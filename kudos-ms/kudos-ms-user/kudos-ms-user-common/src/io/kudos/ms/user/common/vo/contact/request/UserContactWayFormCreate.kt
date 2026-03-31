package io.kudos.ms.user.common.vo.contact.request


/**
 * 用户联系方式表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserContactWayFormCreate (

    override val userId: String? = null,

    override val contactWayDictCode: String? = null,

    override val contactWayValue: String? = null,

    override val contactWayStatusDictCode: String? = null,

    override val priority: Short? = null,

    override val remark: String? = null,

) : IUserContactWayFormBase
