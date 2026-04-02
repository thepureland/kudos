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
    override val id: String,

    override val userId: String?,

    override val contactWayDictCode: String?,

    override val contactWayValue: String?,

    override val contactWayStatusDictCode: String?,

    override val priority: Short?,

    override val remark: String?,

) : IIdEntity<String>, IUserContactWayFormBase
