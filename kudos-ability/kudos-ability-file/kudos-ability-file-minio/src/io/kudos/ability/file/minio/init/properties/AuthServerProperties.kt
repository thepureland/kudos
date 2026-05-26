package io.kudos.ability.file.minio.init.properties

/**
 * Abstract base class for MinIO authentication server configuration classes.
 *
 * Currently the only subclass is [AccessTokenServerProperties] (OAuth2). The base
 * class itself holds no fields — it is kept as an extension point so that new
 * authentication server types (such as LDAP, Kerberos) can extend it in the future.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
abstract class AuthServerProperties
