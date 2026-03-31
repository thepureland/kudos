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

    override val userId: String? = null,

    override val contactWayDictCode: String? = null,

    override val contactWayValue: String? = null,

    override val contactWayStatusDictCode: String? = null,

    override val priority: Short? = null,

    override val remark: String? = null,

) : IIdEntity<String?>, IUserContactWayFormBase
