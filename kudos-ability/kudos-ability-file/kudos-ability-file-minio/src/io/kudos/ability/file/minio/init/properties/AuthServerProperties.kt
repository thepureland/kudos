package io.kudos.ability.file.minio.init.properties

/**
 * MinIO 认证服务器配置类的抽象基类。
 *
 * 目前唯一子类是 [AccessTokenServerProperties]（OAuth2）。基类本身不持有任何字段——保留为
 * 一个扩展点，将来增加新的认证服务器类型（如 LDAP、Kerberos）时各自继承。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
abstract class AuthServerProperties
