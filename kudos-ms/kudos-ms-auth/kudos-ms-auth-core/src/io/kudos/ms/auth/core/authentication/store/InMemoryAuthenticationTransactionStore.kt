package io.kudos.ms.auth.core.authentication.store

import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import java.util.concurrent.ConcurrentHashMap

/**
 * Single-node default store used until a deployment supplies a distributed implementation.
 * Compare-and-set semantics already match the contract required by Redis/JDBC implementations.
 */
open class InMemoryAuthenticationTransactionStore : IAuthenticationTransactionStore {
    private val transactions = ConcurrentHashMap<String, AuthenticationTransaction>()

    override fun create(transaction: AuthenticationTransaction): Boolean =
        transactions.putIfAbsent(transaction.id, transaction) == null

    override fun get(id: String): AuthenticationTransaction? = transactions[id]

    override fun save(
        transaction: AuthenticationTransaction,
        expectedVersion: Long,
    ): AuthenticationTransaction? {
        var saved: AuthenticationTransaction? = null
        transactions.computeIfPresent(transaction.id) { _, current ->
            if (current.version != expectedVersion) {
                current
            } else {
                transaction.copy(version = expectedVersion + 1).also { saved = it }
            }
        }
        return saved
    }
}
