package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.Order
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import org.ktorm.database.Database
import org.ktorm.schema.Table
import org.ktorm.schema.varchar
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


/**
 * DAO fixture for the "does the DAO actually consult the guard" tests.
 *
 * Top-level and `internal` rather than nested and private, because `table()` resolves the table
 * object reflectively through `objectInstance`, which cannot reach a private nested object.
 */
internal interface SecretHolder : IDbEntity<String, SecretHolder> {

    companion object Companion : DbEntityFactory<SecretHolder>()

    var secret: String?
}

internal object SecretHolders : StringIdTable<SecretHolder>("guard_dao_tbl") {
    val secret = encryptedVarchar("secret").bindTo { it.secret }
}

internal open class SecretHolderDao : BaseCrudDao<String, SecretHolder, SecretHolders>() {
    fun sortForTest(order: Order) = allSearch(order)
}

/**
 * Unit tests for [EncryptedColumnGuard] — which queries an encrypted column is allowed to answer.
 *
 * The bug being prevented has no symptom of its own: the parameter is encrypted with the same codec
 * before it reaches the database, so `WHERE secret = 'known value'` compares two ciphertexts. With a
 * randomised codec those never match and the query returns **zero rows**, successfully. Every
 * assertion here is about turning one of those into an error.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class EncryptedColumnGuardTest {

    /** Deterministic: one plaintext, one ciphertext — so exact matches can work. */
    private object ReversingCodec : IStringEncryptionCodec {
        override fun encrypt(plain: String): String = plain.reversed()
        override fun decrypt(stored: String): String = stored.reversed()
        override val deterministic: Boolean get() = true
    }

    /** Randomised, like any real authenticated encryption — and like the shipped default. */
    private class SaltingCodec : IStringEncryptionCodec {
        private var counter = 0
        override fun encrypt(plain: String): String = "${counter++}:$plain"
        override fun decrypt(stored: String): String = stored.substringAfter(':')
    }

    private object Secrets : Table<Nothing>("guard_test_tbl") {
        val plain = varchar("plain")
        val randomised = encryptedVarchar("randomised", SaltingCodec())
        val deterministic = encryptedVarchar("deterministic", ReversingCodec)
    }

    @Test
    fun aPlainColumnIsNeverRestricted() {
        // The guard must be invisible to every table that does not use encryption.
        OperatorEnum.entries.forEach { EncryptedColumnGuard.checkFilterable(Secrets.plain, it) }
        EncryptedColumnGuard.checkSortable(Secrets.plain)
    }

    @Test
    fun equalityOnARandomisedColumnIsRefused() {
        // The headline case: this used to return zero rows and look like "no such user".
        val error = assertFailsWith<IllegalArgumentException> {
            EncryptedColumnGuard.checkFilterable(Secrets.randomised, OperatorEnum.EQ)
        }
        assertTrue(error.message!!.contains("guard_test_tbl.randomised"), "name the column: ${error.message}")
        assertTrue(error.message!!.contains("silently"), "say what would otherwise happen: ${error.message}")
        assertTrue(error.message!!.contains("deterministic"), "name the way out: ${error.message}")
    }

    @Test
    fun exactMatchOperatorsOnARandomisedColumnAreAllRefused() {
        listOf(
            OperatorEnum.EQ, OperatorEnum.NE, OperatorEnum.LG,
            OperatorEnum.IN, OperatorEnum.NOT_IN,
            OperatorEnum.IS_EMPTY, OperatorEnum.IS_NOT_EMPTY,
        ).forEach { operator ->
            assertFailsWith<IllegalArgumentException>("$operator must be refused") {
                EncryptedColumnGuard.checkFilterable(Secrets.randomised, operator)
            }
        }
    }

    @Test
    fun exactMatchOperatorsOnADeterministicColumnAreAllowed() {
        // Determinism is exactly what makes ciphertext-vs-ciphertext equality meaningful, so
        // refusing it too would be a false alarm that pushes callers off the API.
        listOf(OperatorEnum.EQ, OperatorEnum.NE, OperatorEnum.IN, OperatorEnum.NOT_IN)
            .forEach { EncryptedColumnGuard.checkFilterable(Secrets.deterministic, it) }
    }

    @Test
    fun patternAndRangeOperatorsAreRefusedEvenWhenDeterministic() {
        // Determinism does not make ciphertext contain the plaintext's prefixes or preserve its
        // order, so these are impossible for any codec.
        listOf(
            OperatorEnum.LIKE, OperatorEnum.LIKE_S, OperatorEnum.LIKE_E,
            OperatorEnum.ILIKE, OperatorEnum.IEQ,
            OperatorEnum.GT, OperatorEnum.GE, OperatorEnum.LT, OperatorEnum.LE,
            OperatorEnum.BETWEEN, OperatorEnum.NOT_BETWEEN,
        ).forEach { operator ->
            val error = assertFailsWith<IllegalArgumentException>("$operator must be refused") {
                EncryptedColumnGuard.checkFilterable(Secrets.deterministic, operator)
            }
            assertTrue(error.message!!.contains("ciphertext"), "explain why: ${error.message}")
        }
    }

    /**
     * Column-vs-column operators are a different problem, and the deterministic gate does not
     * answer it: such a comparison binds no argument, so no codec ever runs. What decides it is
     * whether both sides are encrypted the same way.
     */
    @Test
    fun columnComparisonsRequireBothSidesEncryptedTheSameWay() {
        // Same deterministic codec on both sides — the only case the database can evaluate.
        EncryptedColumnGuard.checkComparable(Secrets.deterministic, Secrets.deterministic, OperatorEnum.EQ_P)

        // Encrypted against plaintext: ciphertext vs plaintext, silently matches nothing. This is
        // the direction a left-column-only check lets through.
        assertTrue(
            assertFailsWith<IllegalArgumentException> {
                EncryptedColumnGuard.checkComparable(Secrets.deterministic, Secrets.plain, OperatorEnum.EQ_P)
            }.message!!.contains("only one of them is encrypted")
        )
        // …and the mirror image, which a left-column-only check misses entirely.
        assertTrue(
            assertFailsWith<IllegalArgumentException> {
                EncryptedColumnGuard.checkComparable(Secrets.plain, Secrets.deterministic, OperatorEnum.EQ_P)
            }.message!!.contains("only one of them is encrypted")
        )

        // Two encrypted columns with different codecs produce unrelated ciphertexts.
        assertTrue(
            assertFailsWith<IllegalArgumentException> {
                EncryptedColumnGuard.checkComparable(Secrets.deterministic, Secrets.randomised, OperatorEnum.EQ_P)
            }.message!!.contains("different codecs")
        )
        // A randomised codec never matches even against itself.
        assertTrue(
            assertFailsWith<IllegalArgumentException> {
                EncryptedColumnGuard.checkComparable(Secrets.randomised, Secrets.randomised, OperatorEnum.EQ_P)
            }.message!!.contains("different ciphertext each time")
        )
        // Ordering comparisons are meaningless on ciphertext whatever the codecs.
        listOf(OperatorEnum.GT_P, OperatorEnum.GE_P, OperatorEnum.LT_P, OperatorEnum.LE_P).forEach {
            assertFailsWith<IllegalArgumentException>("$it must be refused") {
                EncryptedColumnGuard.checkComparable(Secrets.deterministic, Secrets.deterministic, it)
            }
        }
        // Two plaintext columns are none of this guard's business.
        EncryptedColumnGuard.checkComparable(Secrets.plain, Secrets.plain, OperatorEnum.GT_P)
    }

    @Test
    fun nullChecksAreAlwaysAllowed() {
        // Whether a column is null is not encrypted, so these stay usable — and they are the only
        // way left to query such a column at all.
        listOf(OperatorEnum.IS_NULL, OperatorEnum.IS_NOT_NULL).forEach {
            EncryptedColumnGuard.checkFilterable(Secrets.randomised, it)
            EncryptedColumnGuard.checkFilterable(Secrets.deterministic, it)
        }
    }

    @Test
    fun sortingIsRefusedForEveryCodec() {
        // Unlike filtering, a deterministic codec buys nothing here: ciphertext sorts in ciphertext
        // order either way.
        listOf(Secrets.randomised, Secrets.deterministic).forEach { column ->
            val error = assertFailsWith<IllegalArgumentException> { EncryptedColumnGuard.checkSortable(column) }
            assertTrue(error.message!!.contains("sorted"), error.message!!)
        }
    }

    @Test
    fun theDefaultCodecIsTreatedAsRandomised() {
        // AES-GCM uses a per-call IV. Getting this backwards would re-open the silent-empty-result
        // hole for every column that takes the default.
        assertTrue(!DefaultStringEncryptionCodec.deterministic)
        assertNotNull(
            assertFailsWith<IllegalArgumentException> {
                EncryptedColumnGuard.checkFilterable(DefaultCodecTable.secret, OperatorEnum.EQ)
            }.message
        )
    }

    @Test
    fun aCodecThatSaysNothingIsAssumedRandomised() {
        // The interface defaults `deterministic` to false, so a codec author who never thought about
        // it gets the safe answer rather than silently broken lookups.
        val quiet = object : IStringEncryptionCodec {
            override fun encrypt(plain: String) = plain
            override fun decrypt(stored: String) = stored
        }
        assertTrue(!quiet.deterministic)
    }

    private object DefaultCodecTable : Table<Nothing>("guard_default_tbl") {
        val secret = encryptedVarchar("secret")
    }

    //region reaching the guard through the real query paths

    // The unit assertions above pin the rules; these pin that the DAO actually consults them. A
    // guard nobody calls is the same as no guard, and the paths differ enough that each has its own
    // chance to miss it — Criteria goes through SqlWhereExpressionFactory, `Order` through the sort
    // builder, and `inSearch` builds its `IN` by hand.

    @Test
    fun aCriteriaEqualityOnAnEncryptedColumnIsRefusedByTheDao() {
        val criteria = Criteria.of("secret", OperatorEnum.EQ, "known value")
        val error = assertFailsWith<IllegalArgumentException> {
            CriteriaConverter.convert(criteria, SecretHolders)
        }
        assertTrue(error.message!!.contains("guard_dao_tbl.secret"), error.message!!)
    }

    @Test
    fun sortingByAnEncryptedColumnIsRefusedByTheDao() {
        // allSearch resolves its Database before building the sort, so this path needs one bound —
        // no SQL is ever issued, because the guard rejects the ORDER BY while it is still being
        // assembled.
        KudosContextHolder.get().addOtherInfos(
            KudosContext.OTHER_INFO_KEY_DATABASE to Database.connect("jdbc:h2:mem:guard_sort;DB_CLOSE_DELAY=-1")
        )
        try {
            val error = assertFailsWith<IllegalArgumentException> {
                SecretHolderDao().sortForTest(Order.asc("secret"))
            }
            assertTrue(error.message!!.contains("cannot be sorted on"), error.message!!)
        } finally {
            KudosContextHolder.clear()
        }
    }

    @Test
    fun inSearchOnAnEncryptedColumnIsRefusedByTheDao() {
        val error = assertFailsWith<IllegalArgumentException> {
            SecretHolderDao().inSearch(SecretHolder::secret, listOf("a", "b"))
        }
        assertTrue(error.message!!.contains("guard_dao_tbl.secret"), error.message!!)
    }

    //endregion
}
