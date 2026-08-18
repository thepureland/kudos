package io.kudos.ms.user.core.account.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.user.core.account.model.po.UserAccountProtection
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * Account protection table-entity binding object
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object UserAccountProtections : ManagedTable<UserAccountProtection>("user_account_protection") {

    /** User ID */
    var userId = varchar("user_id").bindTo { it.userId }

    /** Question 1 */
    var question1 = varchar("question1").bindTo { it.question1 }

    /** Answer 1 */
    var answer1 = varchar("answer1").bindTo { it.answer1 }

    /** Question 2 */
    var question2 = varchar("question2").bindTo { it.question2 }

    /** Answer 2 */
    var answer2 = varchar("answer2").bindTo { it.answer2 }

    /** Question 3 */
    var question3 = varchar("question3").bindTo { it.question3 }

    /** Answer 3 */
    var answer3 = varchar("answer3").bindTo { it.answer3 }

    /** Safe contact way ID */
    var safeContactWayId = varchar("safe_contact_way_id").bindTo { it.safeContactWayId }

    /** Total allowed password-recovery attempts */
    var totalValidateCount = int("total_validate_count").bindTo { it.totalValidateCount }

    /** Number of questions that must be answered correctly */
    var matchQuestionCount = int("match_question_count").bindTo { it.matchQuestionCount }

    /** Error count */
    var errorTimes = int("error_times").bindTo { it.errorTimes }




}
