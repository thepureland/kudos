package io.kudos.ability.file.common.auth

/**
 * Marker interface for authentication parameters of file storage backends.
 *
 * Current implementations:
 *  - [AccessKeyServerParam]: AK / SK pair (MinIO / S3 / Aliyun OSS, etc.)
 *  - [AccessTokenServerParam]: single token / Bearer header
 *
 * Designed as an empty interface rather than a sealed class so that downstream
 * storage submodules can define new authentication forms (e.g. STS token with
 * region, Azure SAS URL, etc.).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface AuthServerParam
