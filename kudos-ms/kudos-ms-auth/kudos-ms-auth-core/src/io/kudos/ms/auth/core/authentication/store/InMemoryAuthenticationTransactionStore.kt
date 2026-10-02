package io.kudos.ms.auth.core.authentication.store

import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import java.time.Clock
import java.time.Instant
import java.util.TreeMap

/** Bounded single-node store with real expiry and atomic compare-and-set updates. */
open class InMemoryAuthenticationTransactionStore(
    private val clock: Clock = Clock.systemUTC(),
    private val maxEntries: Int = 10_000,
) : IAuthenticationTransactionStore {
    private val transactions = HashMap<String, AuthenticationTransaction>()
    private val expirations = TreeMap<Instant, MutableSet<String>>()

    init { require(maxEntries > 0) { "Authentication transaction capacity must be positive" } }

    @Synchronized
    override fun create(transaction: AuthenticationTransaction): Boolean {
        expire()
        if (!transaction.expiresAt.isAfter(clock.instant()) || transactions.containsKey(transaction.id)) return false
        check(transactions.size < maxEntries) { "Authentication transaction capacity reached" }
        transactions[transaction.id] = transaction
        expirations.getOrPut(transaction.expiresAt) { mutableSetOf() }.add(transaction.id)
        return true
    }

    @Synchronized
    override fun get(id: String): AuthenticationTransaction? {
        expire()
        return transactions[id]
    }

    @Synchronized
    override fun save(transaction: AuthenticationTransaction, expectedVersion: Long): AuthenticationTransaction? {
        expire()
        val current = transactions[transaction.id] ?: return null
        if (current.version != expectedVersion || !transaction.expiresAt.isAfter(clock.instant())) return null
        // A continuation cannot extend an anonymous transaction's lifetime indefinitely.
        if (transaction.expiresAt != current.expiresAt) return null
        return transaction.copy(version = expectedVersion + 1).also { transactions[it.id] = it }
    }

    private fun expire() {
        val now = clock.instant()
        while (expirations.isNotEmpty() && !expirations.firstKey().isAfter(now)) {
            expirations.pollFirstEntry().value.forEach(transactions::remove)
        }
    }
}
