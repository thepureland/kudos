package io.kudos.ms.user.common.account.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * User account protection cache entry
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountProtectionCacheEntry (

    /** Primary key */
    override val id: String,

    /** User ID */
    val userId: String?,

    /** Question 1 */
    val question1: String?,

    /** Answer 1 */
    val answer1: String?,

    /** Question 2 */
    val question2: String?,

    /** Answer 2 */
    val answer2: String?,

    /** Question 3 */
    val question3: String?,

    /** Answer 3 */
    val answer3: String?,

    /** Safe contact way ID */
    val safeContactWayId: String?,

    /** Total password recovery attempts allowed */
    val totalValidateCount: Int?,

    /** Number of questions that must be answered correctly */
    val matchQuestionCount: Int?,

    /** Error count */
    val errorTimes: Int?,

    /** Remark */
    val remark: String?,

    /** Whether enabled */
    val active: Boolean?,

    /** Whether built-in */
    val builtIn: Boolean?,

    /** Creator ID */
    val createUserId: String?,

    /** Creator name */
    val createUserName: String?,

    /** Create time */
    val createTime: LocalDateTime?,

    /** Updater ID */
    val updateUserId: String?,

    /** Updater name */
    val updateUserName: String?,

    /** Update time */
    val updateTime: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

}
