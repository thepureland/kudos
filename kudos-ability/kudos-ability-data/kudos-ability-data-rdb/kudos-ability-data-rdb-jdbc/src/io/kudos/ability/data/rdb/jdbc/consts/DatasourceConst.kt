package io.kudos.ability.data.rdb.jdbc.consts

interface DatasourceConst {
    companion object {
        const val MODE_MASTER: String = "master"
        const val MODE_READONLY: String = "readonly"
        const val CONSOLE_TENANT_ID: String = "-99"
    }
}
