package io.kudos.ability.data.rdb.jdbc.context

import java.io.Serializable

class DbParam : Serializable {
    /**
     * 强制指定数据源
     */
    var forcedDs: String? = null
    var enableLog: Boolean = false
    var readonly: Boolean = false

    companion object {
        private val serialVersionUID = -3788770245369263297L
    }
}
