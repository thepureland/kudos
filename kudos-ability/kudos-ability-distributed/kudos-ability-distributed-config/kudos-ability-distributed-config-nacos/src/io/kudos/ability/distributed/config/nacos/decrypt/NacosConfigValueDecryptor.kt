package io.kudos.ability.distributed.config.nacos.decrypt

/**
 * Nacos 配置值解密 hook。
 *
 * 默认模块不内置具体加密算法；业务侧可通过 ServiceLoader 注册实现，处理 `ENC(...)`、
 * KMS ciphertext 等自定义格式。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface NacosConfigValueDecryptor {

    fun supports(value: String): Boolean

    fun decrypt(value: String): String
}
