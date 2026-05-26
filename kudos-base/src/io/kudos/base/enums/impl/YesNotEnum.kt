package io.kudos.base.enums.impl

import io.kudos.base.enums.ienums.IDictEnum

/**
 * Logical true/false enum.
 *
 * @author K
 * @since 1.0.0
 */
enum class YesNotEnum(
    val bool: Boolean,
    override val code: String,
    override var displayText: String
) : IDictEnum {

    YES(true, "1", "Yes"),
    NOT(false, "0", "No");

    companion object Companion {

        const val CODE_TABLE_ID = "yes_not"

        fun initTrans(map: Map<String, String>) {
            for (yesNot in entries) {
                yesNot.displayText = map[yesNot.code] ?: yesNot.displayText
            }
        }

        fun enumOf(code: String): YesNotEnum = enumOfBool(code.toBoolean())

        fun enumOfBool(bool: Boolean): YesNotEnum = if (bool) YES else NOT
    }

}