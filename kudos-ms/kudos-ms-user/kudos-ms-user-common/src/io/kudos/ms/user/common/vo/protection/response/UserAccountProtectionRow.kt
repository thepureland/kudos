package io.kudos.ms.user.common.vo.protection.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 用户账号保护列表查询结果响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountProtectionRow (

    /** 主键 */
    override val id: String = "",

    /** 用户ID */
    val userId: String? = null,

    /** 问题1 */
    val question1: String? = null,

    /** 答案1 */
    val answer1: String? = null,

    /** 问题2 */
    val question2: String? = null,

    /** 答案2 */
    val answer2: String? = null,

    /** 问题3 */
    val question3: String? = null,

    /** 答案3 */
    val answer3: String? = null,

    /** 安全联系方式ID */
    val safeContactWayId: String? = null,

    /** 总的找回密码次数 */
    val totalValidateCount: Int? = null,

    /** 必须答对的问题数 */
    val matchQuestionCount: Int? = null,

    /** 错误次数 */
    val errorTimes: Int? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

    /** 创建者ID */
    val createUserId: String? = null,

    /** 创建者名称 */
    val createUserName: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新者ID */
    val updateUserId: String? = null,

    /** 更新者名称 */
    val updateUserName: String? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>