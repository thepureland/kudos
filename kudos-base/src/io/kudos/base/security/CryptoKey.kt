package io.kudos.base.security

/**
 * 密钥常量与全局默认密钥配置入口。
 *
 * 生产环境应在启动阶段调用 [configureDefaultKey] 注入强随机密钥；未配置时使用占位默认值，仅便于本地开发/测试。
 *
 * @author K
 * @since 1.0.0
 */
object CryptoKey {

    private const val PLACEHOLDER_DEFAULT = "io．Kudos．base.security "

    @Volatile
    private var configuredKey: String? = null

    /**
     * 当前全局默认密钥材料。
     * 未通过 [configureDefaultKey] 或赋值写入时，使用内置占位串（勿用于生产）。
     * 赋值接口为兼容旧代码保留；新代码请优先使用 [configureDefaultKey]（含非空校验）。
     */
    var KEY_DEFAULT: String
        get() = configuredKey ?: PLACEHOLDER_DEFAULT
        set(value) {
            configuredKey = value
        }

    /**
     * 在应用启动时配置全局默认密钥（推荐生产使用）。
     *
     * @throws IllegalArgumentException 若 key 为空白
     */
    fun configureDefaultKey(key: String) {
        require(key.isNotBlank()) { "Crypto default key must not be blank" }
        configuredKey = key
    }
}
