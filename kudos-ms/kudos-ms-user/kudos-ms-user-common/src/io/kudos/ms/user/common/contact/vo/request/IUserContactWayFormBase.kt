package io.kudos.ms.user.common.contact.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/**
 * User contact way form base fields (shared between create and update)
 *
 * @author K
 * @since 1.0.0
 */
interface IUserContactWayFormBase {

    /** User ID */
    val userId: String?

    /** Contact way dictionary code */
    val contactWayDictCode: String?

    /** Contact way value */
    val contactWayValue: String?

    /** Contact way status dictionary code */
    val contactWayStatusDictCode: String?

    /** Priority */
    val priority: Short?

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
