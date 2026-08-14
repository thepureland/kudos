package io.kudos.ability.data.rdb.ktorm.rowscope

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Unit tests for [RowScopeWriteValidator] — the rules deciding what a write may store.
 *
 * These are the rules the filter cannot express: an `INSERT` has no existing row to narrow, and an
 * `UPDATE`'s `SET` list is not covered by its `WHERE`. Both decide what a row's scope columns
 * *become*, so both are checks on values rather than conditions on rows.
 *
 * No Spring, no database — the validator is deliberately pure so its rules can be pinned directly.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class RowScopeWriteValidatorTest {

    private interface Order

    private val orgOnly = RowScopePolicy(Order::class, mapOf("org" to "org_id"))
    private val orgAndRegion = RowScopePolicy(Order::class, mapOf("org" to "org_id", "region" to "region_code"))
    private val withCreator = RowScopePolicy(Order::class, mapOf("org" to "org_id"), selfColumn = "create_user_id")

    private fun scope(vararg orgs: String, principal: String? = "alice", self: Boolean = false) =
        RowScope(unrestricted = false, self = self, principalId = principal, dimensions = mapOf("org" to orgs.toSet()))

    //region insert

    @Test
    fun anUnsetColumnIsFilledWhenThereIsOnlyOneAnswer() {
        // The ergonomics the whole feature rests on: a user who belongs to exactly one org should
        // never have to write org_id by hand. Without this, the check gets worked around.
        val defaults = RowScopeWriteValidator.defaultsForInsert(orgOnly, scope("org-a"), mapOf("name" to "x"))
        assertEquals(mapOf<String, Any?>("org_id" to "org-a"), defaults)
    }

    @Test
    fun anUnsetColumnIsAskedAboutWhenThereAreSeveralAnswers() {
        // Guessing here would silently file the row under whichever org sorted first.
        val error = assertFailsWith<RowScopeViolationException> {
            RowScopeWriteValidator.defaultsForInsert(orgOnly, scope("org-a", "org-b"), mapOf("name" to "x"))
        }
        assertTrue(error.message!!.contains("org_id"), error.message!!)
        assertTrue(error.message!!.contains("explicitly"), error.message!!)
    }

    @Test
    fun anExplicitNullIsTreatedAsUnset() {
        // A bean copied into an entity sets every property, most of them to null; treating that as
        // "the caller demanded no org" would reject the most ordinary insert there is.
        val defaults = RowScopeWriteValidator.defaultsForInsert(orgOnly, scope("org-a"), mapOf("org_id" to null))
        assertEquals(mapOf<String, Any?>("org_id" to "org-a"), defaults)
    }

    @Test
    fun aValueInsideTheSubjectsScopeIsAccepted() {
        val defaults = RowScopeWriteValidator.defaultsForInsert(orgOnly, scope("org-a", "org-b"), mapOf("org_id" to "org-b"))
        assertTrue(defaults.isEmpty(), "nothing to fill when the caller said which org")
    }

    @Test
    fun aValueOutsideTheSubjectsScopeIsRefused() {
        // The headline case: the row would exist, be invisible to its own creator, and be entirely
        // real to the other tenant.
        val error = assertFailsWith<RowScopeViolationException> {
            RowScopeWriteValidator.defaultsForInsert(orgOnly, scope("org-a"), mapOf("org_id" to "org-b"))
        }
        assertTrue(error.message!!.contains("org-b"), error.message!!)
        assertTrue(error.message!!.contains("invisible"), error.message!!)
    }

    @Test
    fun eachDeclaredDimensionIsCheckedSeparately() {
        val scope = RowScope(
            unrestricted = false, self = false, principalId = "alice",
            dimensions = mapOf("org" to setOf("org-a"), "region" to setOf("emea", "apac")),
        )
        // org is unambiguous and filled; region has two answers and is asked about.
        val error = assertFailsWith<RowScopeViolationException> {
            RowScopeWriteValidator.defaultsForInsert(orgAndRegion, scope, mapOf("name" to "x"))
        }
        assertTrue(error.message!!.contains("region_code"), error.message!!)
    }

    @Test
    fun aDimensionTheSubjectIsNotRestrictedOnIsLeftAlone() {
        // Matches the filter's reading of an absent or empty value set, so "can read every org" and
        // "can write to every org" stay the same statement.
        val unrestrictedOnOrg = RowScope(unrestricted = false, self = false, principalId = "alice")
        val defaults = RowScopeWriteValidator.defaultsForInsert(orgOnly, unrestrictedOnOrg, mapOf("org_id" to "anything"))
        assertTrue(defaults.isEmpty())
    }

    @Test
    fun theCreatorColumnIsForcedToTheSubject() {
        val defaults = RowScopeWriteValidator.defaultsForInsert(withCreator, scope("org-a"), mapOf("name" to "x"))
        assertEquals("alice", defaults["create_user_id"])
    }

    @Test
    fun attributingARowToAnotherPrincipalIsRefused() {
        // On a SELF-scoped entity this would otherwise hand the row's visibility to someone else —
        // or hide the writer's own rows under another name.
        val error = assertFailsWith<RowScopeViolationException> {
            RowScopeWriteValidator.defaultsForInsert(
                withCreator, scope("org-a"), mapOf("org_id" to "org-a", "create_user_id" to "bob"),
            )
        }
        assertTrue(error.message!!.contains("bob"), error.message!!)
    }

    @Test
    fun columnNamesAreMatchedTheWayTheDatabaseMatchesThem() {
        val defaults = RowScopeWriteValidator.defaultsForInsert(orgOnly, scope("org-a"), mapOf("ORG_ID" to "org-a"))
        assertTrue(defaults.isEmpty(), "an uppercase column name is the same column")
    }

    //endregion

    //region update

    @Test
    fun anUpdateThatDoesNotTouchAScopeColumnIsFine() {
        // The row was already proven visible by the scoped WHERE; renaming it changes nothing about
        // where it belongs.
        RowScopeWriteValidator.checkUpdate(orgOnly, scope("org-a"), mapOf("name" to "renamed"))
    }

    @Test
    fun movingARowOutOfTheSubjectsScopeIsRefused() {
        // The hole that mirrors the insert one: the WHERE proves the caller may touch this row, and
        // says nothing about what they may turn it into. From the caller's side the row simply
        // vanishes; from the other tenant's side it appears.
        val error = assertFailsWith<RowScopeViolationException> {
            RowScopeWriteValidator.checkUpdate(orgOnly, scope("org-a"), mapOf("org_id" to "org-b"))
        }
        assertTrue(error.message!!.contains("org-b"), error.message!!)
    }

    @Test
    fun movingARowWithinTheSubjectsScopeIsAllowed() {
        RowScopeWriteValidator.checkUpdate(orgOnly, scope("org-a", "org-b"), mapOf("org_id" to "org-b"))
    }

    @Test
    fun nullingAScopeColumnIsRefused() {
        // Not a move to another tenant but a move to none, which hides the row from everyone the
        // dimension restricts — the caller included.
        val error = assertFailsWith<RowScopeViolationException> {
            RowScopeWriteValidator.checkUpdate(orgOnly, scope("org-a"), mapOf("org_id" to null))
        }
        assertTrue(error.message!!.contains("runAsSystem"), "say how to do it deliberately: ${error.message}")
    }

    @Test
    fun reassigningTheCreatorIsRefused() {
        val error = assertFailsWith<RowScopeViolationException> {
            RowScopeWriteValidator.checkUpdate(withCreator, scope("org-a"), mapOf("create_user_id" to "bob"))
        }
        assertTrue(error.message!!.contains("runAsSystem"), error.message!!)
    }

    @Test
    fun anUpdateNotMentioningTheCreatorIsFine() {
        RowScopeWriteValidator.checkUpdate(withCreator, scope("org-a"), mapOf("org_id" to "org-a"))
    }

    //endregion
}
