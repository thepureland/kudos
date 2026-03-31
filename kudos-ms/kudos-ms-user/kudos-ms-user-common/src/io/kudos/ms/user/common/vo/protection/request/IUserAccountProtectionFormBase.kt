package io.kudos.ms.user.common.vo.protection.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/**
 * 用户账号保护表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface IUserAccountProtectionFormBase {

    /** 用户ID */
    val userId: String?

    /** 问题1 */
    val question1: String?

    /** 答案1 */
    val answer1: String?

    /** 问题2 */
    val question2: String?

    /** 答案2 */
    val answer2: String?

    /** 问题3 */
    val question3: String?

    /** 答案3 */
    val answer3: String?

    /** 安全联系方式ID */
    val safeContactWayId: String?

    /** 总的找回密码次数 */
    val totalValidateCount: Int?

    /** 必须答对的问题数 */
    val matchQuestionCount: Int?

    /** 错误次数 */
    val errorTimes: Int?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
