package io.kudos.ms.user.common.vo.protection.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 用户账号保护表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountProtectionFormUpdate (

    /** 主键 */
    override val id: String? = null,

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

) : IIdEntity<String?>
