package io.kudos.ms.auth.core.authentication.store

import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction

/** Replaceable short-lived transaction storage; production clusters can provide a Redis implementation. */
interface IAuthenticationTransactionStore {
    fun create(transaction: AuthenticationTransaction): Boolean
    fun get(id: String): AuthenticationTransaction?

    /** Compare-and-set update. Returns the versioned snapshot, or null on a version conflict. */
    fun save(transaction: AuthenticationTransaction, expectedVersion: Long): AuthenticationTransaction?
}
