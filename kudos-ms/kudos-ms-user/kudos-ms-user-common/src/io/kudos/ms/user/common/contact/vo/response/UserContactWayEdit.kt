package io.kudos.ms.user.common.contact.vo.response

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * User contact way edit response VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserContactWayEdit (

    /** Primary key */
    override val id: String = "",

    /** User ID */
    val userId: String? = null,

    /** Contact way dictionary code */
    val contactWayDictCode: String? = null,

    /** Contact way value */
    val contactWayValue: String? = null,

    /** Contact way status dictionary code */
    val contactWayStatusDictCode: String? = null,

    /** Priority */
    val priority: Short? = null,

    /** Remark */
    val remark: String? = null,

) : IIdEntity<String>
