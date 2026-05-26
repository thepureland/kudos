package io.kudos.ms.user.core.account.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * User account protection database entity.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface UserAccountProtection : IDbEntity<String, UserAccountProtection> {

    companion object : DbEntityFactory<UserAccountProtection>()

    /** User id. */
    var userId: String

    /** Question 1. */
    var question1: String

    /** Answer 1. */
    var answer1: String

    /** Question 2. */
    var question2: String?

    /** Answer 2. */
    var answer2: String?

    /** Question 3. */
    var question3: String?

    /** Answer 3. */
    var answer3: String?

    /** Safe contact way id. */
    var safeContactWayId: String?

    /** Total password recovery attempts allowed. */
    var totalValidateCount: Int

    /** Required number of correct answers. */
    var matchQuestionCount: Int

    /** Error count. */
    var errorTimes: Int

    /** Remark. */
    var remark: String?

    /** Active flag. */
    var active: Boolean

    /** Built-in flag. */
    var builtIn: Boolean

    /** Creator id. */
    var createUserId: String?

    /** Creator name. */
    var createUserName: String?

    /** Create time. */
    var createTime: LocalDateTime?

    /** Updater id. */
    var updateUserId: String?

    /** Updater name. */
    var updateUserName: String?

    /** Update time. */
    var updateTime: LocalDateTime?




}
