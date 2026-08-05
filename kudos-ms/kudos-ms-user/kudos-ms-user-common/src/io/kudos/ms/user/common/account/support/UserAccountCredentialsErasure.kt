package io.kudos.ms.user.common.account.support

import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.account.vo.response.UserAccountDetail
import io.kudos.ms.user.common.account.vo.response.UserAccountEdit
import io.kudos.ms.user.common.account.vo.response.UserAccountRow

/**
 * Exit-boundary credential erasure for user account response VOs.
 *
 * The user account VOs carry credential material (BCrypt password hashes, the TOTP secret and the
 * session key) because the in-process passport pipeline needs them for verification. Those fields
 * must NEVER leave the service boundary: a leaked hash enables offline brute force and a leaked
 * TOTP secret enables OTP forgery.
 *
 * Every controller or RPC implementation that serializes one of these VOs to the outside world
 * (admin REST, internal Feign provider, public web) must call the matching function below right
 * before returning. The in-process passport login path keeps reading the unsanitized cache entry
 * or DAO record directly, so verification is unaffected.
 *
 * The functions return defensive copies; the original objects (e.g. shared cache entries) are
 * never mutated.
 *
 * @author K
 * @since 1.0.0
 */

/**
 * Return a copy of this cache entry with all credential fields
 * (loginPassword, securityPassword, authenticationKey, sessionKey) set to null.
 *
 * @return sanitized copy safe for serialization across the service boundary
 */
fun UserAccountCacheEntry.eraseCredentials(): UserAccountCacheEntry = copy(
    loginPassword = null,
    securityPassword = null,
    authenticationKey = null,
    sessionKey = null,
)

/**
 * Return a copy of this detail VO with all credential fields
 * (loginPassword, securityPassword, authenticationKey, sessionKey) set to null.
 *
 * @return sanitized copy safe for serialization across the service boundary
 */
fun UserAccountDetail.eraseCredentials(): UserAccountDetail = copy(
    loginPassword = null,
    securityPassword = null,
    authenticationKey = null,
    sessionKey = null,
)

/**
 * Return a copy of this edit VO with all credential fields
 * (loginPassword, securityPassword, authenticationKey, sessionKey) set to null.
 *
 * @return sanitized copy safe for serialization across the service boundary
 */
fun UserAccountEdit.eraseCredentials(): UserAccountEdit = copy(
    loginPassword = null,
    securityPassword = null,
    authenticationKey = null,
    sessionKey = null,
)

/**
 * Return a copy of this row VO with its credential fields
 * (authenticationKey, sessionKey) set to null. The row VO carries no password hashes.
 *
 * @return sanitized copy safe for serialization across the service boundary
 */
fun UserAccountRow.eraseCredentials(): UserAccountRow = copy(
    authenticationKey = null,
    sessionKey = null,
)
