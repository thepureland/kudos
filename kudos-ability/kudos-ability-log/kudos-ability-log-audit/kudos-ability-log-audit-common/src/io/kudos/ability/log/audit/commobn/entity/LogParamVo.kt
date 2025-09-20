package io.kudos.ability.log.audit.commobn.entity

import io.kudos.ability.log.audit.commobn.enums.LogParamTypeEnum
import java.io.Serializable
import java.util.Locale

/**
 * Create by (admin) on 7/10/15.
 */
class LogParamVo : Serializable {
    //参数名
    var name: String? = null

    //参数值
    var value: Any? = null

    //参数类型
    var type: String? = LogParamTypeEnum.STRING.code

    //参数所属地区(用于货币、日期)
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
