package io.kudos.ms.sys.core.cache.event

/**
 * 缓存配置（`sys_cache`）领域事件。
 *
 * **使用 plain `@EventListener`（非 `@TransactionalEventListener`）**：本域
 * 的服务测试断言 `service.mutation(...)` 后立即可读到新缓存状态。Spring
 * `@Transactional` 自动回滚的单测中 AFTER_COMMIT 不会触发，故此域改用
 * 同步事件以保留旧的直 sync 语义，同时让 service 不再直接依赖 cache 实现。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysCacheEvent {
    val id: String
}

data class SysCacheInserted(override val id: String) : SysCacheEvent

/** 涵盖一般 update、updateActive。 */
data class SysCacheUpdated(override val id: String) : SysCacheEvent

data class SysCacheDeleted(override val id: String) : SysCacheEvent

data class SysCacheBatchDeleted(val ids: Collection<String>) : SysCacheEvent {
    override val id: String get() = ids.first()
}
