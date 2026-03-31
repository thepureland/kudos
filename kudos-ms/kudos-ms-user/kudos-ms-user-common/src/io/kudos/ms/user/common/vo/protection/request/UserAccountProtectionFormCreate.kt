package io.kudos.ms.user.common.vo.protection.request


/**
 * 用户账号保护表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountProtectionFormCreate (

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

) : IUserAccountProtectionFormBase
