package io.kudos.ms.user.common.account.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * User account protection detail response VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountProtectionDetail (

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

    /** Whether enabled */
    val active: Boolean? = null,

    /** Whether built-in */
    val builtIn: Boolean? = null,

    /** Creator ID */
    val createUserId: String? = null,

    /** Creator name */
    val createUserName: String? = null,

    /** Create time */
    val createTime: LocalDateTime? = null,

    /** Updater ID */
    val updateUserId: String? = null,

    /** Updater name */
    val updateUserName: String? = null,

    /** Update time */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>