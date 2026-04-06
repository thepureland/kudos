package io.kudos.ms.user.common.account.vo.request
import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 用户账号保护表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountProtectionFormUpdate (

    /** 主键 */
    override val id: String,

    override val userId: String?,

    override val question1: String?,

    override val answer1: String?,

    override val question2: String?,

    override val answer2: String?,

    override val question3: String?,

    override val answer3: String?,

    override val safeContactWayId: String?,

    override val totalValidateCount: Int?,

    override val matchQuestionCount: Int?,

    override val errorTimes: Int?,

    override val remark: String?,

) : IIdEntity<String>, IUserAccountProtectionFormBase
