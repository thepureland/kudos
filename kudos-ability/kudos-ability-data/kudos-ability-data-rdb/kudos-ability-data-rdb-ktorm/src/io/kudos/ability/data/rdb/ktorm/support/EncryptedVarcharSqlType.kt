package io.kudos.ability.data.rdb.ktorm.support

import io.kudos.base.security.CryptoKit
import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
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
    /** Encrypts the plaintext for storage. Must be deterministic-or-not consistently per call site. */
    fun encrypt(plain: String): String

    /**
     * Decrypts a stored value. Implementations **should** be tolerant of legacy plaintext rows
     * (return the input as-is when no encryption marker is detected); otherwise enabling encryption
     * on a column with historical data would corrupt every existing row at read time.
     */
    fun decrypt(stored: String): String
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
}

/**
 * Ktorm [SqlType] that transparently encrypts a `VARCHAR` column on write and decrypts on read.
 *
 * Mirrors the soul `EncryptHandler` (MyBatis `BaseTypeHandler<String>`) in the Ktorm idiom: encryption
 * happens at the JDBC boundary, so query-side and result-side both see plaintext while the DB only
 * sees ciphertext. Combine with [encryptedVarchar] in your Ktorm table definitions.
 *
 * **Indexing caveat**: encrypted columns cannot be queried with `LIKE`, range comparisons, or
 * server-side functions — the DB sees ciphertext, not the plaintext shape. Equality lookups work
 * **only** when the codec is deterministic (the default AES-GCM uses a per-call IV, so equality
 * lookups against `EncryptedVarcharSqlType(DefaultStringEncryptionCodec)` won't find existing rows).
 * For "encrypted but searchable" columns, use a separate deterministic-hash column for the lookup.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class EncryptedVarcharSqlType(
    private val codec: IStringEncryptionCodec = DefaultStringEncryptionCodec,
) : SqlType<String>(Types.VARCHAR, "varchar") {

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
