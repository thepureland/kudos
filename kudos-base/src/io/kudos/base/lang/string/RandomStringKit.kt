package io.kudos.base.lang.string

import io.kudos.base.lang.string.RandomStringKit.random
import org.apache.commons.lang3.RandomStringUtils
import java.security.SecureRandom
import java.util.Random
import java.util.UUID
import kotlin.math.abs

/**
 * Random string utilities.
 *
 * @author K
 * @since 1.0.0
 */
object RandomStringKit {

    private val random = SecureRandom()

    /**
     * Wraps Java's UUID.
     *
     * @return a UUID delimited with "-"
     * @author K
     * @since 1.0.0
     */
    fun uuid(): String = UUID.randomUUID().toString()

    /**
     * Wraps Java's UUID without "-" delimiters.
     *
     * @return a UUID without "-" delimiters
     * @author K
     * @since 1.0.0
     */
    fun uuidWithoutDelimiter(): String = UUID.randomUUID().toString().replace("-".toRegex(), "")

    /**
     * Randomly generates a Long via SecureRandom.
     *
     * @return random Long
     * @author K
     * @since 1.0.0
     */
    fun randomLong(): String = abs(random.nextLong()).toString()

    /**
     * Randomly generates bytes via SecureRandom, encoded in Base62.
     *
     * @param length length
     * @return Base62-encoded string
     * @author K
     * @since 1.0.0
     */
    fun randomBase62(length: Int): String {
        val randomBytes = ByteArray(length)
        random.nextBytes(randomBytes)
        return EncodeKit.encodeBase62(randomBytes)
    }

    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    // Wrappers around org.apache.commons.lang3.RandomStringUtils
    // vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

    /**
     * Creates a random string of the specified length.
     * Characters are chosen from the entire character set.
     *
     * @param count length of the random string to create
     * @return the random string
     * @author K
     * @since 1.0.0
     */
    fun random(count: Int): String = RandomStringUtils.insecure().next(count)

    /**
     * Creates a random string of the specified length.
     * Characters are chosen from ASCII codes `32` to `126`.
     *
     * @param count length of the random string to create
     * @return the random string
     * @author K
     * @since 1.0.0
     */
    fun randomAscii(count: Int): String = RandomStringUtils.insecure().nextAscii(count)

    /**
     * Creates a random string of the specified length.
     * Characters are chosen from letters.
     *
     * @param count length of the random string to create
     * @return the random string
     * @author K
     * @since 1.0.0
     */
    fun randomAlphabetic(count: Int): String = RandomStringUtils.insecure().nextAlphabetic(count)

    /**
     * Creates a random string of the specified length.
     * Characters are chosen from letters or digits.
     *
     * @param count length of the random string to create
     * @return the random string
     * @author K
     * @since 1.0.0
     */
    fun randomAlphanumeric(count: Int): String = RandomStringUtils.insecure().nextAlphanumeric(count)

    /**
     * Creates a random string of the specified length.
     * Characters are chosen from digits.
     *
     * @param count length of the random string to create
     * @return the random string
     * @author K
     * @since 1.0.0
     */
    fun randomNumeric(count: Int): String = RandomStringUtils.insecure().nextNumeric(count)

    /**
     * Creates a random string of the specified length.
     * Characters are chosen from letters or digits.
     *
     * @param count length of the random string to create
     * @param letters if `true`, generated characters are chosen from letters
     * @param numbers if `true`, generated characters are chosen from digits
     * @return the random string
     * @author K
     * @since 1.0.0
     */
    fun random(count: Int, letters: Boolean, numbers: Boolean): String =
        RandomStringUtils.insecure().next(count, letters, numbers)

    /**
     * Creates a random string of the specified length.
     * Characters are chosen from letters or digits.
     *
     * @param count length of the random string to create
     * @param start start position of the character set range
     * @param end end position of the character set range
     * @param letters if `true`, generated characters are chosen from letters
     * @param numbers if `true`, generated characters are chosen from digits
     * @return the random string
     * @author K
     * @since 1.0.0
     */
    fun random(count: Int, start: Int, end: Int, letters: Boolean, numbers: Boolean): String =
        RandomStringUtils.insecure().next(count, start, end, letters, numbers)

    /**
     * Creates a random string using the default random source.
     * This method has the same semantics as [.random], but does not use a user-provided random source;
     * instead it uses an internal static [Random] instance.
     *
     * @param count length of the random string to create
     * @param start start position of the character set range
     * @param end end position of the character set range
     * @param letters allow letters only?
     * @param numbers allow digits only?
     * @param chars vararg of characters; if `null`, all characters are used
     * @return the random string
     * @throws ArrayIndexOutOfBoundsException if the specified character set contains fewer than `(end - start) + 1` elements
     * @author K
     * @since 1.0.0
     */
    fun random(
        count: Int, start: Int, end: Int, letters: Boolean, numbers: Boolean, vararg chars: Char
    ): String = RandomStringUtils.insecure().next(count, start, end, letters, numbers, *chars)

    /**
     * Creates a random string using the provided random source.
     * If both `start` and `end` are `0`, they will be set to `' '` and `'z'` respectively,
     * and the printable ASCII range will be used, unless both `letters` and `numbers` are `false`,
     * in which case `start` and `end` are set to `0` and `Integer.MAX_VALUE` respectively.
     * If `chars` is not `null`, characters between `start` and `end` are chosen from it.
     * This method accepts a user-provided [Random] instance. Using the same random source with a fixed seed on each call
     * will reproduce the same random string in a predictable manner.
     *
     * @param count length of the random string to create
     * @param start start position of the character set range
     * @param end end position of the character set range
     * @param letters allow letters only?
     * @param numbers allow digits only?
     * @param chars character array; if `null`, all characters are used
     * @param random the random source
     * @return the random string
     * @throws ArrayIndexOutOfBoundsException if the specified character set contains fewer than `(end - start) + 1` elements
     * @throws IllegalArgumentException if `count` &lt; 0.
     * @author K
     * @since 1.0.0
     */
    fun random(
        count: Int, start: Int, end: Int, letters: Boolean, numbers: Boolean, chars: CharArray?, random: Random?
    ): String = RandomStringUtils.random(count, start, end, letters, numbers, chars, random)

    /**
     * Creates a random string of the specified length.
     * Characters are chosen from the given string.
     *
     * @param count length of the random string to create
     * @param chars string providing the character set
     * @return the random string
     * @throws IllegalArgumentException if `count` &lt; 0.
     * @author K
     * @since 1.0.0
     */
    fun random(count: Int, chars: String? = null): String = RandomStringUtils.insecure().next(count, chars)

    /**
     * Creates a random string of the specified length.
     * Characters are chosen from the given character array.
     *
     * @param count length of the random string to create
     * @param chars character array providing the character set; may be null
     * @return the random string
     * @throws IllegalArgumentException if `count` &lt; 0.
     * @author K
     * @since 1.0.0
     */
    fun random(count: Int, vararg chars: Char): String = RandomStringUtils.insecure().next(count, *chars)

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // Wrappers around org.apache.commons.lang3.RandomStringUtils
    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

}
