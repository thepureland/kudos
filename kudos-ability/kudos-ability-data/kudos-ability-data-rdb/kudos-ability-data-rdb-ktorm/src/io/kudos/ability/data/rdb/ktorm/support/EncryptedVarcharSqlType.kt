package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.security.CryptoKit
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.SqlType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * Field-level transparent encryption codec contract.
 *
 * Implementations encrypt on write and decrypt on read for a single string column. The default
 * [DefaultStringEncryptionCodec] is wired to kudos [CryptoKit] (AES-GCM with a prefix marker, and
 * a graceful pass-through for legacy plaintext data on read). Business teams can plug in their own
 * codec for sensitive columns that need a different key, KMS-backed envelope encryption, etc.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IStringEncryptionCodec {
    /** Encrypts the plaintext for storage. */
    fun encrypt(plain: String): String

    /**
     * Decrypts a stored value. Implementations **should** be tolerant of legacy plaintext rows
     * (return the input as-is when no encryption marker is detected); otherwise enabling encryption
     * on a column with historical data would corrupt every existing row at read time.
     */
    fun decrypt(stored: String): String

    /**
     * Whether one plaintext always encrypts to the same ciphertext.
     *
     * This decides whether the column can be *looked up*. A query's parameter is encrypted with this
     * same codec before it reaches the database, so `WHERE secret = ?` compares ciphertext against
     * ciphertext: with a deterministic codec that matches the stored row, and with a randomised one
     * (any sane authenticated encryption, including the AES-GCM default) it never matches anything —
     * the query silently returns nothing rather than failing.
     *
     * **Defaults to false**, which is both the safe answer and the true one for every codec worth
     * using. Override it only if yours really is deterministic, and know what that costs: equal
     * plaintexts become visibly equal in the stored data, which leaks the value distribution to
     * anyone who can read the table.
     */
    val deterministic: Boolean get() = false
}

/**
 * Default codec backed by [CryptoKit.aesEncrypt] / [CryptoKit.aesDecrypt].
 *
 * - Write: `CryptoKit.aesEncrypt(plain)` → AES-GCM hex with `CryptoKit.PREFIX` prefix.
 * - Read: `CryptoKit.aesDecrypt(stored)` → strips the prefix and decrypts; rows without the prefix
 *   are returned unchanged (legacy / migration-friendly).
 */
object DefaultStringEncryptionCodec : IStringEncryptionCodec {
    override fun encrypt(plain: String): String = CryptoKit.aesEncrypt(plain)
    override fun decrypt(stored: String): String = CryptoKit.aesDecrypt(stored)

    /** AES-GCM with a per-call IV, so the same plaintext encrypts differently every time. */
    override val deterministic: Boolean get() = false
}

/**
 * Ktorm [SqlType] that transparently encrypts a `VARCHAR` column on write and decrypts on read.
 *
 * Mirrors the soul `EncryptHandler` (MyBatis `BaseTypeHandler<String>`) in the Ktorm idiom: encryption
 * happens at the JDBC boundary, so query-side and result-side both see plaintext while the DB only
 * sees ciphertext. Combine with [encryptedVarchar] in your Ktorm table definitions.
 *
 * **Querying**: the database only ever sees ciphertext, so `LIKE`, range comparisons, ordering and
 * server-side functions cannot work on such a column, and equality works only when the codec is
 * [IStringEncryptionCodec.deterministic]. Rather than let those silently return nothing, the DAO
 * refuses them — see [EncryptedColumnGuard]. For "encrypted but searchable" columns, keep a separate
 * deterministic-hash column and look up on that.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class EncryptedVarcharSqlType(
    private val codec: IStringEncryptionCodec = DefaultStringEncryptionCodec,
) : SqlType<String>(Types.VARCHAR, "varchar") {

    /** See [IStringEncryptionCodec.deterministic]; decides whether this column can be looked up. */
    val deterministic: Boolean get() = codec.deterministic

    /**
     * Whether [other] encrypts with the same codec, which is what decides if the two columns'
     * ciphertexts can be compared to each other at all. Identity, not equality: two codec instances
     * of the same class may still hold different keys.
     */
    internal fun sharesCodecWith(other: EncryptedVarcharSqlType): Boolean = codec === other.codec

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: String) {
        ps.setString(index, codec.encrypt(parameter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): String? {
        val raw = rs.getString(index) ?: return null
        return codec.decrypt(raw)
    }
}

/**
 * Registers an [EncryptedVarcharSqlType] column on this Ktorm [BaseTable]. Drop-in replacement for
 * `varchar(name)` when the column needs at-rest encryption — call sites read and write plaintext,
 * the DB stores ciphertext.
 *
 * Usage:
 * ```kotlin
 * object UserAccount : ManagedTable<UserAccount>("user_account") {
 *     val loginPassword = encryptedVarchar("login_password").bindTo { it.loginPassword }
 *     // or with a custom codec:
 *     val secret = encryptedVarchar("secret", KmsBackedCodec).bindTo { it.secret }
 * }
 * ```
 */
fun BaseTable<*>.encryptedVarchar(
    name: String,
    codec: IStringEncryptionCodec = DefaultStringEncryptionCodec,
): Column<String> = registerColumn(name, EncryptedVarcharSqlType(codec))


/**
 * Refuses the queries an [encryptedVarchar] column cannot actually answer.
 *
 * Every one of them fails the same way without this: the database compares the *ciphertext* the
 * codec produced for the query parameter against the *ciphertext* stored in the row. With a
 * randomised codec — which is every codec you should be using — those never match, so
 * `WHERE secret = 'known value'` returns **zero rows** and reports no error at all. Ordering and
 * `LIKE` are worse: they return rows, ordered or filtered by properties of the ciphertext, which
 * looks like a working query and is not.
 *
 * Silently answering the wrong thing is the failure mode this module refuses everywhere else, so it
 * is refused here too, with a message that says which column and what to do instead.
 *
 * **Scope.** This covers the conditions and ordering the DAO builds — [Criteria], search payloads
 * and `Order`. A caller who genuinely wants to match raw ciphertext (a migration, say) can still
 * write it through ktorm's own DSL, which does not pass through here.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object EncryptedColumnGuard {

    /** Comparing null-ness never touches the value, so ciphertext is irrelevant. */
    private val NULL_CHECKS = setOf(OperatorEnum.IS_NULL, OperatorEnum.IS_NOT_NULL)

    /**
     * Operators that reduce to "is this exactly that value", which the database can answer on
     * ciphertext — but only if one plaintext always produces one ciphertext.
     */
    private val EXACT_MATCHES = setOf(
        OperatorEnum.EQ, OperatorEnum.NE, OperatorEnum.LG,
        OperatorEnum.IN, OperatorEnum.NOT_IN,
        OperatorEnum.IS_EMPTY, OperatorEnum.IS_NOT_EMPTY,
    )

    /**
     * Rejects [operator] on [column] when the column is encrypted and the operator cannot work.
     *
     * @param column the column being filtered on; anything but an encrypted one returns immediately
     * @param operator the operator the caller asked for
     * @throws IllegalArgumentException when the query could only ever return the wrong rows
     */
    fun checkFilterable(column: ColumnDeclaring<*>, operator: OperatorEnum) {
        val sqlType = column.sqlType as? EncryptedVarcharSqlType ?: return
        if (operator in NULL_CHECKS) return
        val name = columnNameOf(column)
        require(operator in EXACT_MATCHES) {
            "Operator [$operator] cannot be used on encrypted column [$name]: the database only ever " +
                "sees ciphertext, so pattern matching and range comparisons describe the ciphertext " +
                "rather than the value. Decrypt and filter in memory, or keep a separate searchable " +
                "column alongside the encrypted one."
        }
        require(sqlType.deterministic) {
            "Operator [$operator] cannot be used on encrypted column [$name]: its codec produces a " +
                "different ciphertext each time the same value is encrypted, so the encrypted query " +
                "parameter never equals the encrypted stored value and the query would silently " +
                "match nothing. Add a deterministic hash column and look up on that, or — only if " +
                "your codec really maps one value to one ciphertext — override " +
                "IStringEncryptionCodec.deterministic to true."
        }
    }

    /**
     * Rejects ordering by [column] when it is encrypted — unlike filtering, this is refused whatever
     * the codec does, because ciphertext sorts in ciphertext order and a deterministic codec does
     * not make that any closer to the plaintext order the caller asked for.
     *
     * @param column the column being sorted on
     * @throws IllegalArgumentException when the column is encrypted
     */
    fun checkSortable(column: ColumnDeclaring<*>) {
        if (column.sqlType !is EncryptedVarcharSqlType) return
        throw IllegalArgumentException(
            "Encrypted column [${columnNameOf(column)}] cannot be sorted on: the database orders the " +
                "stored ciphertext, which bears no relation to the order of the values it hides. " +
                "Sort the decrypted results in memory, or sort on a non-encrypted column."
        )
    }

    /**
     * Rejects a column-against-column comparison that involves an encrypted column.
     *
     * These are a different problem from comparing against a value, and [checkFilterable] cannot
     * answer them: a column-vs-column comparison **binds no argument**, so no codec ever runs and
     * "is the codec deterministic" is not the question. What matters is whether the two sides are
     * encrypted the same way. They are refused unless both carry the *same* deterministic codec
     * instance, which is the only case the database can actually evaluate:
     *
     * - one side encrypted and the other not compares ciphertext to plaintext — never matches;
     * - two different codecs (or keys) produce unrelated ciphertexts — never matches;
     * - a randomised codec never matches even against itself;
     * - ordering comparisons are meaningless on ciphertext whatever the codecs.
     *
     * @param left the left-hand column
     * @param right the right-hand column named by the criterion's value
     * @param operator the column-vs-column operator
     * @throws IllegalArgumentException when the comparison could only ever return the wrong rows
     */
    fun checkComparable(left: ColumnDeclaring<*>, right: ColumnDeclaring<*>, operator: OperatorEnum) {
        val leftType = left.sqlType as? EncryptedVarcharSqlType
        val rightType = right.sqlType as? EncryptedVarcharSqlType
        if (leftType == null && rightType == null) return

        val names = "[${columnNameOf(left)}] and [${columnNameOf(right)}]"
        require(operator == OperatorEnum.EQ_P || operator == OperatorEnum.NE_P) {
            "Operator [$operator] cannot compare $names: at least one is encrypted, and ordering " +
                "comparisons describe the ciphertext rather than the values it hides."
        }
        require(leftType != null && rightType != null) {
            "Operator [$operator] cannot compare $names: only one of them is encrypted, so this " +
                "compares ciphertext against plaintext and would silently match nothing."
        }
        require(leftType.sharesCodecWith(rightType)) {
            "Operator [$operator] cannot compare $names: they are encrypted with different codecs, " +
                "so their ciphertexts are unrelated and the comparison would silently match nothing."
        }
        require(leftType.deterministic) {
            "Operator [$operator] cannot compare $names: their codec produces a different ciphertext " +
                "each time, so even equal values compare unequal and the query would silently match " +
                "nothing."
        }
    }

    private fun columnNameOf(column: ColumnDeclaring<*>): String =
        (column as? Column<*>)?.let { "${it.table.tableName}.${it.name}" } ?: column.toString()
}
