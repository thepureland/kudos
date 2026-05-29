package io.kudos.ability.web.guest.provider

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.consts.CacheKey
import io.kudos.ability.web.guest.init.properties.GuestProperties
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions

/**
 * Redis-backed [IGuestAccessStore].
 *
 * Storage scheme — one key per active visitor:
 *  - Key: `{prefix}:{hash}` (composed via [CacheKey.getCacheKey] so the separator stays
 *    consistent with the rest of kudos's cache modules)
 *  - Value: the payload [Map] from [GuestAccess.payload], serialised by whatever serializer the
 *    selected [RedisTemplate] is wired with (kudos default is JSON via Jackson)
 *  - TTL: [GuestRepository.timeout][io.kudos.ability.web.guest.init.properties.GuestRepository.timeout]
 *
 * Write semantics (mirrors soul):
 *  - Key already exists → simple `EXPIRE` to roll the TTL. No listener fired.
 *  - Key absent → `SET` with TTL, then notify [IGuestAccessListener.active] (a single signal per
 *    "visitor becomes live" transition; existing TTL rolls don't double-fire).
 *
 * The scan-based [count] uses [ScanOptions] with batch size 1000 to avoid the `KEYS *` antipattern
 * — a busy production Redis with 100k+ active visitors would lock up under `KEYS`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class RedisGuestAccessStore(
    private val properties: GuestProperties,
    private val redisTemplates: RedisTemplates,
    /** Optional listener; null means "no one is interested in the active signal". */
    private val listener: IGuestAccessListener?,
) : IGuestAccessStore {

    override fun store(guestAccess: GuestAccess) {
        val hash = guestAccess.hash
            ?: error("GuestAccess.hash must be populated before store(); call IGuestAccessService.hash first")
        val timeout = properties.repository.timeout
        val key = composeKey(hash)
        val template = template()
        if (template.hasKey(key) == true) {
            template.expire(key, timeout)
        } else {
            template.opsForValue().set(key, guestAccess.payload ?: emptyMap<String, String>(), timeout)
            listener?.active(guestAccess)
        }
    }

    override fun count(): GuestAccessStat {
        val stat = GuestAccessStat()
        val pattern = composeKey("*")
        template().scan(ScanOptions.scanOptions().count(1000).match(pattern).build()).use { cursor ->
            while (cursor.hasNext()) {
                cursor.next()
                stat.count += 1
            }
        }
        return stat
    }

    override fun getByHash(key: String): GuestAccess? {
        @Suppress("UNCHECKED_CAST")
        val payload = template().boundValueOps(composeKey(key)).get() as? Map<String, String> ?: return null
        return GuestAccess().apply {
            this.hash = key
            this.payload = payload
        }
    }

    /** Wipe every guest key under the configured prefix — test-only helper. */
    fun clearForTest() {
        val pattern = composeKey("*")
        val template = template()
        template.scan(ScanOptions.scanOptions().count(1000).match(pattern).build()).use { cursor ->
            while (cursor.hasNext()) {
                template.unlink(cursor.next())
            }
        }
    }

    private fun template(): RedisTemplate<Any, Any?> {
        val groupName = properties.repository.groupName
        return if (groupName.isNullOrBlank()) {
            redisTemplates.defaultRedisTemplate
        } else {
            redisTemplates.getRedisTemplate(groupName)
                ?: error("RedisTemplate not found for groupName=$groupName; check kudos.ability.data.redis.redis-map")
        }
    }

    private fun composeKey(suffix: String): String =
        CacheKey.getCacheKey(properties.repository.prefix, suffix)
}
