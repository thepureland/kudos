package io.kudos.ms.user.common.account.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 用户账号保护缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountProtectionCacheEntry (

    /** 主键 */
    override val id: String,

    /** 用户ID */
    val userId: String?,

    /** 问题1 */
    val question1: String?,

    /** 答案1 */
    val answer1: String?,

    /** 问题2 */
    val question2: String?,

    /** 答案2 */
    val answer2: String?,

    /** 问题3 */
    val question3: String?,

    /** 答案3 */
    val answer3: String?,

    /** 安全联系方式ID */
    val safeContactWayId: String?,

    /** 总的找回密码次数 */
    val totalValidateCount: Int?,

    /** 必须答对的问题数 */
    val matchQuestionCount: Int?,

    /** 错误次数 */
    val errorTimes: Int?,

    /** 备注 */
    val remark: String?,

    /** 是否启用 */
    val active: Boolean?,

    /** 是否内置 */
    val builtIn: Boolean?,

    /** 创建者ID */
    val createUserId: String?,

    /** 创建者名称 */
    val createUserName: String?,

    /** 创建时间 */
    val createTime: LocalDateTime?,

    /** 更新者ID */
    val updateUserId: String?,

    /** 更新者名称 */
    val updateUserName: String?,

    /** 更新时间 */
    val updateTime: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

}
