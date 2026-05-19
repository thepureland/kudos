package io.kudos.ability.comm.websocket.ktor.session

import io.kudos.base.logger.LogFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * 进程级 WebSocket 会话注册中心。
 *
 * 三套索引同步维护：
 *  - `sessionId → session`（主索引）
 *  - `userId → Set<sessionId>`（一个用户可能多端在线）
 *  - `tenantId → Set<sessionId>`（按租户广播）
 *
 * **不**持久化、**不**跨进程同步——单实例部署够用；多实例部署的"全局广播"需要业务侧自己
 * 做（典型方案：每条业务消息走 Redis pub/sub，每个进程的本类各自再广播给本进程内 session）。
 *
 * 线程安全：所有索引用 `ConcurrentHashMap` + 内部 `Set` 也是并发的 [java.util.Collections.newSetFromMap]。
 * 写操作（[register] / [unregister]）会同时更新三个索引；为简化语义没有原子化整体——会有
 * 极短的"主索引已写但二级索引还没写"窗口，业务侧广播代码不要假设强原子性。
 *
 * @author K
 * @since 1.0.0
 */
class KudosWebSocketRegistry {

    private val log = LogFactory.getLog(this::class)

    private val byId = ConcurrentHashMap<String, KudosWebSocketSessionRef>()
    private val byUserId = ConcurrentHashMap<String, MutableSet<String>>()
    private val byTenantId = ConcurrentHashMap<String, MutableSet<String>>()

    /** 已注册的会话总数。 */
    val size: Int get() = byId.size

    /**
     * 把一个会话加入注册中心。重复 [KudosWebSocketSessionRef.sessionId] 会覆盖原条目
     * （不强制——业务侧只要保证 sessionId 唯一）。
     */
    fun register(session: KudosWebSocketSessionRef) {
        byId[session.sessionId] = session
        session.userId?.let { uid ->
            byUserId.computeIfAbsent(uid) { newConcurrentSet() }.add(session.sessionId)
        }
        session.tenantId?.let { tid ->
            byTenantId.computeIfAbsent(tid) { newConcurrentSet() }.add(session.sessionId)
        }
        log.debug("注册 WebSocket 会话 sessionId={0} userId={1} tenantId={2} total={3}",
            session.sessionId, session.userId, session.tenantId, size)
    }

    /**
     * 从注册中心剔除一个会话。**不会**主动 close 连接——调用方决定关闭时机
     * （典型用法：路由的 `finally` 里调本方法）。
     */
    fun unregister(sessionId: String) {
        val session = byId.remove(sessionId) ?: return
        session.userId?.let { uid ->
            byUserId.compute(uid) { _, ids ->
                ids?.remove(sessionId)
                if (ids.isNullOrEmpty()) null else ids
            }
        }
        session.tenantId?.let { tid ->
            byTenantId.compute(tid) { _, ids ->
                ids?.remove(sessionId)
                if (ids.isNullOrEmpty()) null else ids
            }
        }
        log.debug("注销 WebSocket 会话 sessionId={0} userId={1} tenantId={2} total={3}",
            sessionId, session.userId, session.tenantId, size)
    }

    /** 按 sessionId 取会话，没找到返回 `null`。 */
    fun findById(sessionId: String): KudosWebSocketSessionRef? = byId[sessionId]

    /** 按 userId 取所有会话快照（不是 live view，遍历时不会被并发修改影响）。 */
    fun findByUserId(userId: String): List<KudosWebSocketSessionRef> =
        byUserId[userId].orEmpty().mapNotNull { byId[it] }

    /** 按 tenantId 取所有会话快照。 */
    fun findByTenantId(tenantId: String): List<KudosWebSocketSessionRef> =
        byTenantId[tenantId].orEmpty().mapNotNull { byId[it] }

    /** 当前全部会话的快照。 */
    fun all(): List<KudosWebSocketSessionRef> = byId.values.toList()

    private fun newConcurrentSet(): MutableSet<String> =
        java.util.Collections.newSetFromMap(ConcurrentHashMap())
}
