package io.kudos.ms.user.common.contact.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.user.common.contact.vo.response.UserContactWayRow


/**
 * User contact way list query request VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserContactWayQuery (

    /** User ID */
    val userId: String? = null,

    /** Contact way dictionary code */
    val contactWayDictCode: String? = null,

    /** Contact way value */
    val contactWayValue: String? = null,

    /** Contact way status dictionary code */
    val contactWayStatusDictCode: String? = null,

    /** Whether active */
    val active: Boolean? = null,

    /** Whether built-in */
    val builtIn: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = UserContactWayRow::class

}