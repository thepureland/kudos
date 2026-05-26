package io.kudos.ms.user.common.account.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/**
 * User account protection form base fields (shared between create and update)
 *
 * @author K
 * @since 1.0.0
 */
interface IUserAccountProtectionFormBase {

    /** User ID */
    val userId: String?

    /** Question 1 */
    val question1: String?

    /** Answer 1 */
    val answer1: String?

    /** Question 2 */
    val question2: String?

    /** Answer 2 */
    val answer2: String?

    /** Question 3 */
    val question3: String?

    /** Answer 3 */
    val answer3: String?

    /** Safe contact way ID */
    val safeContactWayId: String?

    /** Total password recovery attempts allowed */
    val totalValidateCount: Int?

    /** Number of questions that must be answered correctly */
    val matchQuestionCount: Int?

    /** Error count */
    val errorTimes: Int?

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
