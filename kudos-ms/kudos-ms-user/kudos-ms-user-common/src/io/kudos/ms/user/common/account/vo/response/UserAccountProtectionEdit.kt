package io.kudos.ms.user.common.account.vo.response

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * User account protection edit response VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountProtectionEdit (

    /** Primary key */
    override val id: String = "",

    /** User ID */
    val userId: String? = null,

    /** Question 1 */
    val question1: String? = null,

    /** Answer 1 */
    val answer1: String? = null,

    /** Question 2 */
    val question2: String? = null,

    /** Answer 2 */
    val answer2: String? = null,

    /** Question 3 */
    val question3: String? = null,

    /** Answer 3 */
    val answer3: String? = null,

    /** Safe contact way ID */
    val safeContactWayId: String? = null,

    /** Total password recovery attempts allowed */
    val totalValidateCount: Int? = null,

    /** Number of questions that must be answered correctly */
    val matchQuestionCount: Int? = null,

    /** Error count */
    val errorTimes: Int? = null,

    /** Remark */
    val remark: String? = null,

) : IIdEntity<String>
