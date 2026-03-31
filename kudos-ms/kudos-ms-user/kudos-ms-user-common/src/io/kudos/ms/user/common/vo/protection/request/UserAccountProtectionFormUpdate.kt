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

    override val userId: String? = null,

    override val question1: String? = null,

    override val answer1: String? = null,

    override val question2: String? = null,

    override val answer2: String? = null,

    override val question3: String? = null,

    override val answer3: String? = null,

    override val safeContactWayId: String? = null,

    override val totalValidateCount: Int? = null,

    override val matchQuestionCount: Int? = null,

    override val errorTimes: Int? = null,

    override val remark: String? = null,

) : IIdEntity<String?>, IUserAccountProtectionFormBase
