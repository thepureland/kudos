package io.kudos.ms.user.core.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.user.core.model.po.UserAccountProtection
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * 用户账号保护数据库表-实体关联对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
//region your codes 1
object UserAccountProtections : StringIdTable<UserAccountProtection>("user_account_protection") {
//endregion your codes 1

    /** 用户ID */
    var userId = varchar("user_id").bindTo { it.userId }

    /** 问题1 */
    var question1 = varchar("question1").bindTo { it.question1 }

    /** 答案1 */
    var answer1 = varchar("answer1").bindTo { it.answer1 }

    /** 问题2 */
    var question2 = varchar("question2").bindTo { it.question2 }

    /** 答案2 */
    var answer2 = varchar("answer2").bindTo { it.answer2 }

    /** 问题3 */
    var question3 = varchar("question3").bindTo { it.question3 }

    /** 答案3 */
    var answer3 = varchar("answer3").bindTo { it.answer3 }

    /** 安全联系方式ID */
    var safeContactWayId = varchar("safe_contact_way_id").bindTo { it.safeContactWayId }

    /** 总的找回密码次数 */
    var totalValidateCount = int("total_validate_count").bindTo { it.totalValidateCount }

    /** 必须答对的问题数 */
    var matchQuestionCount = int("match_question_count").bindTo { it.matchQuestionCount }

    /** 错误次数 */
    var errorTimes = int("error_times").bindTo { it.errorTimes }

    /** 备注 */
    var remark = varchar("remark").bindTo { it.remark }

    /** 是否启用 */
    var active = boolean("active").bindTo { it.active }

    /** 是否内置 */
    var builtIn = boolean("built_in").bindTo { it.builtIn }

    /** 创建者ID */
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    /** 创建者名称 */
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }

    /** 创建时间 */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** 更新者ID */
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** 更新者名称 */
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** 更新时间 */
    var updateTime = datetime("update_time").bindTo { it.updateTime }


    //region your codes 2

    //endregion your codes 2

}
