package io.kudos.base.enums.impl

import io.kudos.base.enums.ienums.IDictEnum

/**
 * 逻辑真假的枚举
 *
 * @author K
 * @since 1.0.0
 */
enum class YesNotEnum(val bool: Boolean, override val code: String, override var trans: String) : IDictEnum {

    YES(true, "1", "是"),
    NOT(false, "0", "否");

    companion object Companion {

        const val CODE_TABLE_ID = "yes_not"

        fun initTrans(map: Map<String, String>) {
            for (yesNot in entries) {
                yesNot.trans = map[yesNot.code]!!
            }
        }

        fun enumOf(code: String): YesNotEnum = enumOfBool(code.toBoolean())

        fun enumOfBool(bool: Boolean): YesNotEnum = if (bool) YES else NOT
    }

    init {
        this.trans = trans
    }
}