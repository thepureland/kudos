package io.kudos.ms.auth.core.version.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.auth.core.version.model.po.AuthPrincipalVersion
import io.kudos.ms.auth.core.version.model.table.AuthPrincipalVersions
import org.springframework.stereotype.Repository


/**
 * Token-epoch DAO.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Repository
open class AuthPrincipalVersionDao : BaseCrudDao<String, AuthPrincipalVersion, AuthPrincipalVersions>() {

    /**
     * The principal's epoch row, or null when it has never been bumped.
     *
     * Absence is the normal case and means epoch 0 — rows are created lazily so the overwhelming
     * majority of principals cost nothing.
     *
     * @param principalId the principal
     * @return the row, or null
     */
    open fun findByPrincipalId(principalId: String): AuthPrincipalVersion? =
        search(Criteria(AuthPrincipalVersion::principalId eq principalId)).firstOrNull()

    /**
     * Takes an exclusive row lock on the principal, creating its row if absent, and holds it until
     * the surrounding transaction ends.
     *
     * **What this is for.** Every grant write reads the principal's effective role set, checks it
     * against the duty-separation rules, and then inserts — a read-then-write with nothing spanning
     * the two. The only database constraint on `auth_role_user` is `unique(role_id, user_id)`, which
     * stops the same role being bound twice and cannot express "not both A and B". So two
     * transactions binding the two halves of an exclusive pair each observe the other's absence and
     * both commit: textbook write skew, and separation of duties exists precisely for the
     * adversarial case where somebody issues both requests deliberately.
     *
     * Serialising on the *principal* rather than on the roles is what makes this correct. The
     * conflict is a property of the receiving principal's resulting role set, not of either role, so
     * a lock on either role would still let two different pairs slip past. It also fixes the whole
     * family of read-then-write races on one principal's grants, not just this instance of it.
     *
     * This table is the natural lock target: it already holds exactly one row per principal, it is
     * owned by this module, and its rows are cheap. Locking `user_account` instead would reach into
     * another module's table and invite deadlocks with ordinary account updates.
     *
     * Contention is a non-issue: grants to one principal are rare administrative acts, and only
     * writers ever take this lock — decisions read caches and never come here.
     *
     * Must be called inside a transaction; the lock is released when it commits or rolls back.
     *
     * @param principalId the principal whose grants are about to change
     */
    open fun lockPrincipal(principalId: String) {
        require(principalId.isNotBlank()) { "principalId must not be blank." }
        // An UPDATE, not a `SELECT ... FOR UPDATE`. The select form is the textbook answer but its
        // locking behaviour varies by engine and mode — under H2's MVStore it did not serialise the
        // writers at all, and a lock that silently does not lock is worse than none, because the
        // code reads as if the race were handled. An UPDATE takes a genuine row write-lock
        // everywhere, held until the transaction ends, which is exactly the semantics wanted here.
        repeat(LOCK_ATTEMPTS) {
            if (touch(principalId) > 0) return
            // No row visible yet: create it, then take the lock on it next time round. Concurrent
            // creators race here and the unique index resolves that race rather than us — whoever
            // loses waits for the winner to commit and then locks the row the winner created.
            runCatching {
                insert(AuthPrincipalVersion {
                    this.principalId = principalId
                    this.epoch = 0L
                })
            }
        }
        // Failing closed is the whole point. The previous shape swallowed the contention and then
        // carried on without the lock, which is strictly worse than not locking at all: the code
        // reads as though the race were handled while the guarantee is quietly absent. Refusing the
        // write costs one retryable error; proceeding costs a breached duty-separation rule.
        throw IllegalStateException(
            "could not acquire the grant lock for principal ${principalId} after ${LOCK_ATTEMPTS} " +
                "attempts; the write is refused rather than performed unguarded — retry it.",
        )
    }

    private companion object {
        /**
         * Enough to outlast a competing creator committing, few enough that a genuinely stuck lock
         * surfaces as an error rather than a hang.
         */
        const val LOCK_ATTEMPTS = 3
    }

    /**
     * Write-touches the principal's row and reports how many rows it affected.
     *
     * `update_time` is the column touched because it is the one whose meaning ("this row was last
     * written at") stays true no matter why the lock was taken.
     */
    private fun touch(principalId: String): Int = database().useConnection { connection ->
        connection.prepareStatement(
            """update "auth_principal_version" set "update_time" = ? where "principal_id" = ?""",
        ).use { statement ->
            statement.setTimestamp(1, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()))
            statement.setString(2, principalId)
            statement.executeUpdate()
        }
    }

    /**
     * Every stored epoch, as `principalId -> epoch`. Used to warm the cache in one read rather than
     * one per principal; the table only holds principals that have actually been bumped, so it stays
     * far smaller than the account table.
     *
     * @return map of principal id to epoch
     */
    open fun searchAllEpochsForCache(): Map<String, Long> =
        allSearch().associate { it.principalId to it.epoch }
}
