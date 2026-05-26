package io.kudos.base.security

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale

/**
 * Purpose: encrypt the provided string with a supplied key, and decrypt the ciphertext produced under the same rules.
 * Notes: encrypt and decrypt are perfectly symmetric, so you can also treat decrypt as the encryption function and
 *        use encrypt to restore the original.
 * Usage: myEncrypt is an example; in practice you can invoke encrypt/outOrder multiple times as needed;
 *        when decrypting, invoke decrypt/deOutOrder in the same order to restore the original.
 * Requirements: the input string may contain only digits and letters; the key is a positive integer no longer than 18 digits.
 *
 * Revision history
 * Version  Date        Author    Operation
 * 1.00    2016/04/22  Leisure   Created this class, including encrypt and decrypt functions
 * 1.10    2016/04/22  Leisure   Added character-conversion; the ciphertext is no longer a meaningful combination of characters
 * 1.11    2016/04/22  Leisure   Changed key from int to Long; key length raised from a maximum of 9 digits to 18 digits
 * 1.12    2016/04/23  Leisure   Refactored the string-sorting method
 * 1.20    2016/04/23  Leisure   Added MD5 check bit
 * 1.21    2016/04/23  Leisure   Fixed a bug: use a custom encoding to keep the ciphertext format compatible with the source
 * 1.22    2016/04/23  Leisure   Character conversion handles both upper and lower case
 * 1.23    2016/04/25  Leisure   Moved the check bit into myEncrypt/myDecrypt; the original encrypt/decrypt functions remain symmetric
 * 1.24    2016/04/25  Leisure   A combination of uppercase letters and digits still maps back to uppercase letters and digits
 * 2.00    2013/04/26  Leisure   Renamed functions to avoid ambiguity; changed function scope. Hands-off version
 */
object Base36Kit {

    /** Default encryption key (18-digit positive integer), used when no key is explicitly supplied. */
    const val KEY = 999966699996669999L

    /**
     * Encrypts the source string and prepends a single check character.
     * The source string must contain only uppercase letters and digits; lowercase letters are treated as uppercase.
     * Uses the default key.
     * @param src
     * @return
     */
    fun encryptIgnoreCase(src: String): String {
        return encryptIgnoreCase(src, KEY)
    }

    /**
     * Encrypts the source string and prepends a single check character.
     * The source string must contain only uppercase letters and digits; lowercase letters are treated as uppercase.
     * @param src
     * @param key
     * @return
     */
    fun encryptIgnoreCase(src: String, key: Long): String { // treat lowercase letters as uppercase
        var srcString = src
        srcString = srcString.uppercase(Locale.getDefault())
        // generate the check bit
        val checkBit = requireNotNull(getMD5(srcString)) { "MD5 computation failed" }.substring(0, 1)
        // encrypt
        val targStr = encrypt(srcString, key, true)
        // prepend the check bit
        return checkBit + targStr
    }

    /**
     * Decrypts an encrypted string that includes a check bit, and verifies it against the check bit.
     * Uses the default key.
     * @param srcString
     * @return
     */
    @Deprecated(
        message = "Prefer tryDecryptIgnoreCase to handle check-bit failure semantics explicitly",
        replaceWith = ReplaceWith("tryDecryptIgnoreCase(srcString, KEY).getOrThrow()")
    )
    fun decryptIgnoreCase(srcString: String): String {
        return tryDecryptIgnoreCase(srcString, KEY).getOrElse { ex ->
            if (ex.message == "Check bit mismatch!") "Check bit mismatch!" else throw ex
        }
    }

    /**
     * Decrypts an encrypted string that includes a check bit, and verifies it against the check bit.
     * @param src
     * @param key
     * @return
     */
    @Deprecated(
        message = "Prefer tryDecryptIgnoreCase to handle check-bit failure semantics explicitly",
        replaceWith = ReplaceWith("tryDecryptIgnoreCase(src, key).getOrThrow()")
    )
    fun decryptIgnoreCase(src: String, key: Long): String { // extract the check bit
        var srcString = src
        val checkBit = srcString.substring(0, 1)
        // extract the ciphertext
        srcString = srcString.substring(1, srcString.length)
        // decrypt
        val targStr = decrypt(srcString, key, true)
        // verify the check bit
        return if (checkBit != requireNotNull(getMD5(targStr)) { "MD5 computation failed" }.substring(0, 1)) {
            "Check bit mismatch!"
        } else targStr
    }

    /**
     * Decrypts an encrypted string with a check bit and returns a Result.
     *
     * The difference from `decryptIgnoreCase` is that a check-bit mismatch returns Failure rather than a fixed error string.
     */
    fun tryDecryptIgnoreCase(src: String, key: Long = KEY): Result<String> = runCatching {
        var srcString = src
        val checkBit = srcString.substring(0, 1)
        srcString = srcString.substring(1, srcString.length)
        val targStr = decrypt(srcString, key, true)
        require(checkBit == requireNotNull(getMD5(targStr)) { "MD5 computation failed" }.substring(0, 1)) { "Check bit mismatch!" }
        targStr
    }

    /**
     * Encryption function.
     * @param src
     * @param key
     * @param capitalOnly
     * @return
     */
    fun encrypt(src: String, key: Long, capitalOnly: Boolean): String { // shuffle
        val targStr = outOrder(src, key)
        // string conversion
        return transStr(targStr, key, true, capitalOnly)
    }

    /**
     * Decryption function.
     * @param src
     * @param key
     * @param capitalOnly
     * @return
     */
    fun decrypt(src: String, key: Long, capitalOnly: Boolean): String { // string conversion
        var srcString = src
        srcString = transStr(srcString, key, false, capitalOnly)
        // restore the order
        return deOutOrder(srcString, key)
    }

    /**
     * "Shuffles" the source string by key:
     * 1. Rotate left by (key % 100 % len) positions;
     * 2. Use each digit of the key to produce sort weights and reorder the first min(srcLen, keyLen) characters by weight;
     * 3. Tail characters past the key length retain the relative order after the rotation.
     *
     * Strictly inverse to [deOutOrder].
     *
     * @param src the string to shuffle
     * @param key the encryption key
     * @return the shuffled string
     * @author K
     * @since 1.0.0
     */
    private fun outOrder(src: String, key: Long): String {
        var srcString = src
        val keyStr = key.toString()
        //String[] keyArr = keyStr.split("");
        val len1 = srcString.length
        val len2 = keyStr.length
        val skipNum = (key % 100 % len1).toInt()
        val len = if (len1 < len2) len1 else len2
        var num = Array(len) { arrayOfNulls<String>(3) }
        // rotate the string
        srcString = (srcString + srcString).substring(skipNum, skipNum + len1)
        // key conversion
        val keyArr = IntArray(keyStr.length)
        for (i in keyStr.indices) {
            keyArr[i] = keyStr.substring(i, i + 1).toInt()
            keyArr[i] = 100 - (10 * keyArr[i] + i)
        }
        for (i in 0 until len) {
            num[i][0] = i.toString()
            num[i][1] = keyArr[i].toString()
            num[i][2] = srcString.substring(i, i + 1)
        }
        // reorder the array by the key column
        num = sortArr(num, 1)
        var targStr = String()
        for (i in 0 until len) {
            targStr += num[i][2]
        }
        targStr += srcString.substring(len, len1)
        return targStr
    }

    /**
     * Inverse of [outOrder]: first sort by key weight to obtain the position mapping,
     * then place ciphertext characters back into their original positions, and finally
     * apply the reverse rotation to restore the original string.
     *
     * @param srcString the shuffled string
     * @param key the encryption key (must match the one used for encryption)
     * @return the string restored to the original order
     * @author K
     * @since 1.0.0
     */
    private fun deOutOrder(srcString: String, key: Long): String {
        val keyStr = key.toString()
        //String[] keyArr = keyStr.split("");
        val len1 = srcString.length
        val len2 = keyStr.length
        val skipNum = (key % 100 % len1).toInt()
        val len = if (len1 < len2) len1 else len2
        var num = Array(len) { arrayOfNulls<String>(3) }
        // key conversion
        val keyArr = IntArray(keyStr.length)
        for (i in keyStr.indices) {
            keyArr[i] = keyStr.substring(i, i + 1).toInt()
            keyArr[i] = 100 - (10 * keyArr[i] + i)
        }
        for (i in 0 until len) {
            num[i][0] = i.toString()
            num[i][1] = keyArr[i].toString()
            //num[i][2] = srcString.substring(i, i+1);
        }
        // first reorder the array by the key column
        num = sortArr(num, 1)
        // insert characters into the array
        for (i in 0 until len) {
            num[i][2] = srcString.substring(i, i + 1)
        }
        // reorder the array by the index column
        num = sortArr(num, 0)
        var targStr = String()
        for (i in 0 until len) {
            targStr += num[i][2]
        }
        targStr += srcString.substring(len, len1)
        // rotate the string
        targStr = (targStr + targStr).substring(len1 - skipNum, 2 * len1 - skipNum)
        return targStr
    }

    /**
     * Performs an in-place ascending bubble sort on a 2D string array by a given column.
     * The sort key is the integer parsed from the string; an empty column value triggers [IllegalArgumentException].
     *
     * @param arr the array to sort; rows are elements, columns are properties
     * @param col the column index used for comparison
     * @return the sorted array (same reference as the input)
     * @author K
     * @since 1.0.0
     */
    private fun sortArr(arr: Array<Array<String?>>, col: Int): Array<Array<String?>> {
        var temp: Array<String?>
        for (i in 0 until arr.size - 1) {
            for (j in arr.size - 1 downTo i + 1) {
                val left = requireNotNull(arr[j][col]) { "Sort column value is empty" }.toInt()
                val right = requireNotNull(arr[j - 1][col]) { "Sort column value is empty" }.toInt()
                if (left < right) {
                    temp = arr[j]
                    arr[j] = arr[j - 1]
                    arr[j - 1] = temp
                }
            }
        }
        return arr
    }

    /**
     * Performs additive/subtractive shifts on the string prefix within a custom encoding space (base 36 or base 62).
     * Flow: ASCII -> custom encoding -> add or subtract the corresponding key digit -> back to ASCII.
     * When band36=true, lowercase letters are treated as uppercase so that a "pure uppercase+digits" input still maps back to pure uppercase+digits.
     *
     * @param inStr the string to convert
     * @param transNum the number used for the shift (i.e. the encryption key)
     * @param plusMinus true for encryption (addition), false for decryption (subtraction)
     * @param band36 true to use base 36 (uppercase + digits only), false to use base 62 (includes lowercase)
     * @return the converted string
     * @author K
     * @since 1.0.0
     */
    private fun transStr(inStr: String, transNum: Long, plusMinus: Boolean, band36: Boolean): String {
        var s = inStr
        var band = 62
        if (band36) {
            band = 36
            // base 36 excludes lowercase letters, so they are treated as uppercase
            s = s.uppercase(Locale.getDefault())
        }
        val len1 = s.length
        val len2 = transNum.toString().length
        val len = if (len1 < len2) len1 else len2
//        val outStr = String()
        val ch = s.toCharArray()
        for (i in 0 until len) {
            val j = len1 - 1 - i
            // convert to the custom base-36 or base-62 encoding
            ch[j] = asciiToDiy(ch[j].code).toChar()
            if (plusMinus) { //System.out.print((int)ch[j] + " " + ch[j] + " ");
                ch[j] = ((ch[j].code + transNum.toString().substring(i, i + 1).toInt()) % band).toChar()
                //System.out.print((int)ch[j] + " " + ch[j] + " ");
            } else { //System.out.print((int)ch[j] + " " + ch[j] + " ");
                ch[j] =
                    ((ch[j].code - transNum.toString().substring(i, i + 1).toInt() + band) % band).toChar()
                //System.out.print((int)ch[j] + " " + ch[j] + " ");
            }
            // convert the custom encoding back to ASCII
            ch[j] = diyToAscii(ch[j].code).toChar()
        }
        return String(ch)
    }

    /**
     * Maps an ASCII code to the custom compact encoding:
     * digits `0-9` -> 0..9, uppercase letters `A-Z` -> 10..35, lowercase letters `a-z` -> 36..61,
     * other characters are left unchanged.
     *
     * @param codeNum the ASCII code of the input character
     * @return the custom encoding value
     * @author K
     * @since 1.0.0
     */
    private fun asciiToDiy(codeNum: Int): Int { // if n's ASCII is in 48..57 it's a digit; 65..90 is an uppercase letter
        return when(codeNum) {
            in 48..57 -> codeNum - 48
            in 65..90 -> codeNum - 65 + 10
            in 97..122 -> codeNum - 97 + 36
            else -> codeNum
        }
    }

    /**
     * Inverse of [asciiToDiy]: maps the custom compact encoding back to ASCII.
     * Note: subtraction may leave codeNum temporarily at -1 (band-modulo underflow),
     * in which case it falls into the `-1..9` branch and maps into the `0-9` digit range, preserving inversion.
     *
     * @param codeNum the custom encoding value
     * @return the corresponding ASCII code
     * @author K
     * @since 1.0.0
     */
    private fun diyToAscii(codeNum: Int): Int { // convert custom encoding back to ASCII
        return when(codeNum) {
            in -1..9 -> codeNum + 48
            in 10..36 -> codeNum - 10 + 65
            in 37..62 -> codeNum - 36 + 97
            else -> codeNum
        }
    }

    /**
     * Computes the MD5 digest of a string and returns an uppercase hex string.
     * Used only to generate a one-character check bit at the head of the encryption result; not for general security use.
     *
     * @param s the string to digest
     * @return 32-character uppercase hex MD5; returns null if the JDK does not support MD5
     * @author K
     * @since 1.0.0
     */
    private fun getMD5(s: String): String? {
        val hexDigits =
            charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
        return try {
            val btInput = s.toByteArray()
            // Obtain a MessageDigest object for the MD5 algorithm
            val mdInst = MessageDigest.getInstance("MD5")
            // Update the digest with the given bytes
            mdInst.update(btInput)
            // Obtain the ciphertext
            val md = mdInst.digest()
            // Convert the ciphertext to a hexadecimal string
            val j = md.size
            val str = CharArray(j * 2)
            var k = 0
            for (i in 0 until j) {
                val byte0 = md[i]
                str[k++] = hexDigits[byte0.toInt().ushr(4) and 0xf]
                str[k++] = hexDigits[byte0.toInt() and 0xf]
            }
            String(str)
        } catch (_: NoSuchAlgorithmException) {
            null
        }
    }

}