package io.kudos.ability.distributed.config.nacos.decrypt

/**
 * Nacos config value decryption hook.
 *
 * The module ships no built-in encryption algorithm; application code can register implementations
 * via ServiceLoader to handle `ENC(...)`, KMS ciphertext, and other custom formats.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface NacosConfigValueDecryptor {

    fun supports(value: String): Boolean

    fun decrypt(value: String): String
}
