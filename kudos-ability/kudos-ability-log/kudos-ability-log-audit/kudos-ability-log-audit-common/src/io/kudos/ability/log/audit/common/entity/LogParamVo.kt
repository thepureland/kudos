package io.kudos.ability.log.audit.common.entity

import io.kudos.ability.log.audit.common.enums.LogParamTypeEnum
import java.io.Serializable
import java.util.Locale

/**
 * Description-parameter POJO matching the `${name}` placeholders in log templates.
 *
 * A single parameter may carry [type] (string / currency / date) and [locale] (for currency / date formatting).
 * The business side adds them via `BaseLog.addParam(LogParamVo(...))` or `addParam(name, value)`.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class LogParamVo : Serializable {
    // Parameter name
    var name: String? = null

    // Parameter value
    var value: Any? = null

    // Parameter type
    var type: String? = LogParamTypeEnum.STRING.code

    // Locale of the parameter (used for currency / date)
    private var locale: Locale? = null

    constructor()

    constructor(name: String?, value: Any?) {
        this.name = name
        this.value = value
    }

    constructor(name: String?, value: Any?, type: String?) : this(name, value) {
        this.type = type
    }

    constructor(name: String?, value: Any?, type: String?, locale: Locale?) : this(name, value, type) {
        this.locale = locale
    }

    fun getLocale(): Locale? {
        return locale
    }

    fun setLocale(locale: Locale?) {
        this.locale = locale
    }

    companion object {
        const val serialVersionUID: Long = 2073240960229763633L
    }
}
