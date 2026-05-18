package io.kudos.base.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/**
 * 密码哈希工具：基于 BCrypt 的单向哈希 + 验证。
 *
 * 适用于**用户登录密码**类需要长期存储的凭据，**不要**用 [CryptoKit] 的 AES 来存密码——
 * AES 是可逆的，密钥一旦泄露则全表明文。BCrypt 含随机 salt、不可逆，且 cost 因子可在硬件
 * 增强后通过 [strength] 上调而不影响旧 hash 的可验证性。
 *
 * 形态：
 * - hash 输出形如 `$2a$10$...`（60 字符固定长度）
 * - 含算法版本 + cost + salt + hash，自描述；后续 [matches] 不需要单独传 salt
 *
 * 线程安全：[BCryptPasswordEncoder] 实例本身线程安全；这里保留单例。
 *
 * @author K
 * @since 1.0.0
 */
object PasswordKit {

    /** BCrypt cost 因子；10 是 2026 年合理的服务端默认（约 100ms/hash on modern CPU）。 */
    const val DEFAULT_STRENGTH = 10

    private val DEFAULT_ENCODER = BCryptPasswordEncoder(DEFAULT_STRENGTH)

    /**
     * 计算明文密码的 BCrypt hash。每次调用产生不同输出（salt 随机）。
     *
     * @param plainPassword 明文密码，非空
     * @return BCrypt hash 字符串（60 字符），可直接落库
     */
    fun hash(plainPassword: String): String {
        require(plainPassword.isNotEmpty()) { "明文密码不能为空" }
        return DEFAULT_ENCODER.encode(plainPassword) ?: error("BCryptPasswordEncoder.encode 返回 null")
    }

    /**
     * 用指定 cost 因子计算 hash。仅在迁移或测试场景下需要显式覆盖。
     */
    fun hash(plainPassword: String, strength: Int): String {
        require(plainPassword.isNotEmpty()) { "明文密码不能为空" }
        return BCryptPasswordEncoder(strength).encode(plainPassword) ?: error("BCryptPasswordEncoder.encode 返回 null")
    }

    /**
     * 校验明文密码是否匹配存储的 hash。
     *
     * @param plainPassword 用户提供的明文密码
     * @param storedHash 数据库中存储的 BCrypt hash 字符串
     * @return true 匹配；false 不匹配或 hash 格式不合法（不抛异常）
     */
    fun matches(plainPassword: String, storedHash: String?): Boolean {
        if (storedHash.isNullOrEmpty()) return false
        return try {
            DEFAULT_ENCODER.matches(plainPassword, storedHash)
        } catch (e: IllegalArgumentException) {
            // BCryptPasswordEncoder 对非法 hash 格式会抛 IAE；这里吞掉并返回 false，方便登录流统一处理
            false
        }
    }

    /**
     * 判断给定字符串是否看起来像 BCrypt hash（用于鉴别历史的非 BCrypt 数据）。
     */
    fun looksLikeBcryptHash(value: String?): Boolean {
        if (value == null) return false
        // BCrypt 输出：$2a$ / $2b$ / $2y$ 前缀 + cost + salt + hash，总长 60
        return value.length == 60 && value.matches(Regex("""^\$2[abxy]\$\d{2}\$.{53}$"""))
    }
}
