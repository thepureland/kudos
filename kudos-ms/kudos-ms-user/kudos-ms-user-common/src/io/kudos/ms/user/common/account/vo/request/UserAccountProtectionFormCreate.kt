package io.kudos.ms.user.common.account.vo.request

/**
 * 用户账号保护表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountProtectionFormCreate (

    override val userId: String? ,

    override val question1: String? ,

    override val answer1: String? ,

    override val question2: String? ,

    override val answer2: String? ,

    override val question3: String? ,

    override val answer3: String? ,

    override val safeContactWayId: String? ,

    override val totalValidateCount: Int? ,

    override val matchQuestionCount: Int? ,

    override val errorTimes: Int? ,

    override val remark: String? ,

) : IUserAccountProtectionFormBase
