package io.kudos.context.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 协程上下文元素，用于把 [KudosContext] 透传给挂起函数链。
 *
 * Spring MVC 经由 ThreadLocal 跨方法传上下文，但在协程中切线程会丢失；
 * 把上下文塞进 [CoroutineContext.Key] 后，`withContext` / `launch` 自动随协程跳跃。
 *
 * @property value 当前协程持有的 [KudosContext]
 * @author K
 * @since 1.0.0
 */
class KudosContextElement(val value: KudosContext) : AbstractCoroutineContextElement(Key) {
    /** [KudosContextElement] 在 [CoroutineContext] 中的 key */
    companion object Key : CoroutineContext.Key<KudosContextElement>
}

/** ========== 保留：协程侧获取与包装 ========== */

/**
 * 在 suspend 函数里取当前协程绑定的 [KudosContext]。
 * 协程中未设置上下文时抛错，确保业务代码不会拿到 fallback 的空上下文。
 *
 * @return 当前协程上的 [KudosContext]
 * @throws IllegalStateException 协程上下文不含 [KudosContextElement] 时
 * @author K
 * @since 1.0.0
 */
suspend fun currentKudosContext(): KudosContext =
    currentCoroutineContext()[KudosContextElement]?.value
        ?: error("KudosContext is absent in coroutineContext")

/**
 * 如果当前协程已经带 [KudosContext]，直接执行 [block]；
 * 否则用 [provider] 生成一个并包到 [withContext] 中再执行。
 *
 * 适合"网关入口"或"消息消费入口"——已有上下文则继承，没有就兜底创建。
 *
 * @param T [block] 的返回类型
 * @param provider 缺省上下文生成函数，默认构造空 [KudosContext]
 * @param block 待执行的挂起逻辑
 * @return [block] 的返回值
 * @author K
 * @since 1.0.0
 */
suspend inline fun <T> withKudosContextIfAbsent(
    crossinline provider: (CoroutineContext) -> KudosContext = { KudosContext() },
    crossinline block: suspend () -> T
): T {
    val elem = currentCoroutineContext()[KudosContextElement]
    return if (elem != null) block()
    else withContext(KudosContextElement(provider(currentCoroutineContext()))) { block() }
}

/**
 * 用显式 [ctx] 覆盖当前协程的 [KudosContext] 并执行 [block]。
 * 适合"模拟另一个用户/租户"等需要替换整段上下文的场景。
 *
 * @param T [block] 的返回类型
 * @param ctx 显式上下文
 * @param block 待执行的挂起逻辑
 * @return [block] 的返回值
 * @author K
 * @since 1.0.0
 */
suspend inline fun <T> withKudosContext(
    ctx: KudosContext,
    crossinline block: suspend () -> T
): T = withContext(KudosContextElement(ctx)) { block() }

/**
 * 在已有 [CoroutineScope] 上启动协程时显式附带 [KudosContext]。
 * 等价于 `scope.launch(extraContext + KudosContextElement(ctx)) { ... }` 的便捷封装。
 *
 * @param ctx 要附带的 [KudosContext]
 * @param context 其它 [CoroutineContext] 元素，默认空
 * @param block 协程体
 * @return 新启动的 [kotlinx.coroutines.Job]
 * @author K
 * @since 1.0.0
 */
fun CoroutineScope.launchWithKudos(
    ctx: KudosContext,
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
) = launch(context + KudosContextElement(ctx), block = block)


