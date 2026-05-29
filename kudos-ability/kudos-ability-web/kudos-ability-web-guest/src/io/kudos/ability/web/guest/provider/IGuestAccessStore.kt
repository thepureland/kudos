package io.kudos.ability.web.guest.provider

/**
 * Persistence contract for visitor records.
 *
 * Default impl: [RedisGuestAccessStore]. Apps with custom backends (in-memory for tests,
 * Cassandra/JDBC for cross-DC aggregation, etc.) declare their own bean implementing this
 * contract and the auto-config's `@ConditionalOnMissingBean` lets them take over.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IGuestAccessStore {

    /**
     * Persist or refresh the record for the given visitor.
     *
     * Implementations should treat this as an upsert + TTL roll: if the visitor already exists,
     * extend their TTL; if not, write the payload and notify [IGuestAccessListener.active].
     */
    fun store(guestAccess: GuestAccess)

    /**
     * Aggregate snapshot of currently-active visitors. Implementations should avoid full keyspace
     * scans where possible (the Redis default uses [SCAN] not [KEYS]).
     */
    fun count(): GuestAccessStat

    /** Read back a single visitor record by [GuestAccess.hash]. Returns null when absent. */
    fun getByHash(key: String): GuestAccess?
}
