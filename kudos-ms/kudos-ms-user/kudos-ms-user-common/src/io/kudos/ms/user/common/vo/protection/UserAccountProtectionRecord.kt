package io.kudos.ms.user.common.vo.protection

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 用户账号保护查询记录
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class UserAccountProtectionRecord (

    //region your codes 1

    /** 用户ID */
    var userId: String? = null,

    /** 问题1 */
    var question1: String? = null,

    /** 答案1 */
    var answer1: String? = null,

    /** 问题2 */
    var question2: String? = null,

    /** 答案2 */
    var answer2: String? = null,

    /** 问题3 */
    var question3: String? = null,

    /** 答案3 */
    var answer3: String? = null,

    /** 安全联系方式ID */
    var safeContactWayId: String? = null,

    /** 总的找回密码次数 */
    var totalValidateCount: Int? = null,

    /** 必须答对的问题数 */
    var matchQuestionCount: Int? = null,

    /** 错误次数 */
    var errorTimes: Int? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    /** 创建者ID */
    var createUserId: String? = null,

    /** 创建者名称 */
    var createUserName: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新者ID */
    var updateUserId: String? = null,

    /** 更新者名称 */
    var updateUserName: String? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    //endregion your codes 3

}
