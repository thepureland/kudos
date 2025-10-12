package io.kudos.context.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/** 协程元素：用于 Ktor 插件/协程传播 */
class KudosContextElement(val value: KudosContext) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<KudosContextElement>
}

/** ========== 保留：协程侧获取与包装 ========== */

/** 在 suspend 中获取当前协程里的 KudosContext；若没有则抛错 */
suspend fun currentKudosContext(): KudosContext =
    currentCoroutineContext()[KudosContextElement]?.value
        ?: error("KudosContext is absent in coroutineContext")

/** 如果协程里没有上下文，则用 provider 生成一个并包一层，否则直接执行 */
suspend inline fun <T> withKudosContextIfAbsent(
    crossinline provider: (CoroutineContext) -> KudosContext = { KudosContext() },
    crossinline block: suspend () -> T
): T {
    val elem = currentCoroutineContext()[KudosContextElement]
    return if (elem != null) block()
    else withContext(KudosContextElement(provider(currentCoroutineContext()))) { block() }
}

/** 显式覆盖/设置协程上下文 */
suspend inline fun <T> withKudosContext(
    ctx: KudosContext,
    crossinline block: suspend () -> T
): T = withContext(KudosContextElement(ctx)) { block() }

/** 在已有 CoroutineScope 上启动协程时显式附带 KudosContext */
fun CoroutineScope.launchWithKudos(
    ctx: KudosContext,
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
) = launch(context + KudosContextElement(ctx), block = block)


