package io.kudos.ms.user.common.contact.vo.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * User contact way form update request VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserContactWayFormUpdate (

    /** Primary key */
    override val id: String,

    override val userId: String?,

    override val contactWayDictCode: String?,

    override val contactWayValue: String?,

    override val contactWayStatusDictCode: String?,

    override val priority: Short?,

    override val remark: String?,

) : IIdEntity<String>, IUserContactWayFormBase
