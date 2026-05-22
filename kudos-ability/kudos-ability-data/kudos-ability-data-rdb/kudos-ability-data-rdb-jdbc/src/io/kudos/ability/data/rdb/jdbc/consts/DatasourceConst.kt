package io.kudos.ability.data.rdb.jdbc.consts

/**
 * 数据源相关字面量常量集合。
 *
 * 用接口 + companion 是单例常量在 Kotlin 里的传统打包方式；外部代码可以用
 * `DatasourceConst.MODE_MASTER` 直接引用。新增常量请保持"字符串字面量 + kdoc 说明语义"。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface DatasourceConst {
    companion object {

        /** 主库模式标记。Master 模式承担写 + 强一致性读，是数据源 key 的默认后缀。 */
        const val MODE_MASTER: String = "master"

        /** 只读副本模式标记。只读路径附加为数据源 key 后缀，由 `DbParam.readonly=true` 触发。 */
        const val MODE_READONLY: String = "readonly"

        /** "控制台租户"约定 id。代表全局管理员身份，跳过普通租户级数据源路由（直走主库）。 */
        const val CONSOLE_TENANT_ID: String = "-99"
    }
}
