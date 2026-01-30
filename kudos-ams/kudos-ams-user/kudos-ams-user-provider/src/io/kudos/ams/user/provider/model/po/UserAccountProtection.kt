package io.kudos.ams.user.provider.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 用户账号保护数据库实体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
interface UserAccountProtection : IDbEntity<String, UserAccountProtection> {
//endregion your codes 1

    companion object : DbEntityFactory<UserAccountProtection>()

    /** 用户ID */
    var userId: String

    /** 问题1 */
    var question1: String

    /** 答案1 */
    var answer1: String

    /** 问题2 */
    var question2: String?

    /** 答案2 */
    var answer2: String?

    /** 问题3 */
    var question3: String?

    /** 答案3 */
    var answer3: String?

    /** 安全联系方式ID */
    var safeContactWayId: String?

    /** 总的找回密码次数 */
    var totalValidateCount: Int

    /** 必须答对的问题数 */
    var matchQuestionCount: Int

    /** 错误次数 */
    var errorTimes: Int

    /** 备注 */
    var remark: String?

    /** 是否启用 */
    var active: Boolean

    /** 是否内置 */
    var builtIn: Boolean

    /** 创建者ID */
    var createUserId: String?

    /** 创建者名称 */
    var createUserName: String?

    /** 创建时间 */
    var createTime: LocalDateTime?

    /** 更新者ID */
    var updateUserId: String?

    /** 更新者名称 */
    var updateUserName: String?

    /** 更新时间 */
    var updateTime: LocalDateTime?


    //region your codes 2

    //endregion your codes 2

}
