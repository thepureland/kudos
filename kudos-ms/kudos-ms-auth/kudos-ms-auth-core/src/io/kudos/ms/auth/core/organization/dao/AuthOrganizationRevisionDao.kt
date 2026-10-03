package io.kudos.ms.auth.core.organization.dao

import io.kudos.ability.data.rdb.ktorm.datasource.currentDatabase
import io.kudos.context.core.KudosContextHolder
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.map
import org.ktorm.dsl.plus
import org.ktorm.dsl.select
import org.ktorm.dsl.update
import org.ktorm.dsl.where
import org.ktorm.schema.Table
import org.ktorm.schema.long
import org.ktorm.schema.varchar
import org.springframework.stereotype.Repository

/** `auth_organization_revision`: one monotonic revision per organization. */
object AuthOrganizationRevisions : Table<Nothing>("auth_organization_revision") {
    val organizationId = varchar("organization_id").primaryKey()
    val revision = long("revision")
}

/**
 * Organization authorization revision.
 *
 * Every change to an organization's authorization facts runs [lock] first and [bump] before
 * committing, on the same transaction-bound connection as the change itself. Locking the row
 * serializes concurrent writers of one organization, so a check made after [lock] (SoD, last
 * administrator, expected revision) cannot be invalidated by a concurrent commit.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Repository
open class AuthOrganizationRevisionDao {

    private fun database() = KudosContextHolder.currentDatabase()

    /** Current revision; 0 when the organization never changed. */
    open fun current(organizationId: String): Long =
        database().from(AuthOrganizationRevisions)
            .select(AuthOrganizationRevisions.revision)
            .where { AuthOrganizationRevisions.organizationId eq organizationId }
            .map { it[AuthOrganizationRevisions.revision] ?: 0L }
            .firstOrNull() ?: 0L

    /** Takes the organization's row lock for the rest of the transaction and returns its revision. */
    open fun lock(organizationId: String): Long {
        val touched = database().update(AuthOrganizationRevisions) {
            set(it.revision, it.revision)
            where { it.organizationId eq organizationId }
        }
        if (touched == 0) {
            // A failed duplicate insert would abort a PostgreSQL transaction, so the first row is created
            // with a statement that cannot fail on a concurrent first writer.
            database().useConnection { connection ->
                val sql = if (connection.metaData.databaseProductName.contains("PostgreSQL", ignoreCase = true)) {
                    "insert into auth_organization_revision (organization_id, revision) values (?, 0) on conflict (organization_id) do nothing"
                } else {
                    "merge into auth_organization_revision t using (values (?)) s(id) on t.organization_id = s.id " +
                        "when not matched then insert (organization_id, revision) values (s.id, 0)"
                }
                connection.prepareStatement(sql).use { it.setString(1, organizationId); it.executeUpdate() }
            }
            // Whether we inserted or a concurrent first writer did, the row exists now.
            database().update(AuthOrganizationRevisions) {
                set(it.revision, it.revision)
                where { it.organizationId eq organizationId }
            }
        }
        return current(organizationId)
    }

    /** Increments the revision (call after [lock], in the same transaction) and returns the new value. */
    open fun bump(organizationId: String): Long {
        database().update(AuthOrganizationRevisions) {
            set(it.revision, it.revision + 1L)
            where { it.organizationId eq organizationId }
        }
        return current(organizationId)
    }
}
