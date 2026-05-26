package io.kudos.context.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Coroutine context element used to propagate [KudosContext] through a chain of suspending functions.
 *
 * Spring MVC propagates context across methods via ThreadLocal, but switching threads inside coroutines loses it;
 * once the context is placed into a [CoroutineContext.Key], `withContext` / `launch` automatically carry it across
 * coroutine hops.
 *
 * @property value The [KudosContext] held by the current coroutine
 * @author K
 * @since 1.0.0
 */
class KudosContextElement(val value: KudosContext) : AbstractCoroutineContextElement(Key) {
    /** Key for [KudosContextElement] in a [CoroutineContext] */
    companion object Key : CoroutineContext.Key<KudosContextElement>
}

/** ========== Retained: coroutine-side retrieval and wrapping ========== */

/**
 * Retrieve the [KudosContext] bound to the current coroutine inside a suspend function.
 * Throws when no context is set on the coroutine, ensuring business code never receives a fallback empty context.
 *
 * @return The [KudosContext] on the current coroutine
 * @throws IllegalStateException When the coroutine context does not contain [KudosContextElement]
 * @author K
 * @since 1.0.0
 */
suspend fun currentKudosContext(): KudosContext =
    currentCoroutineContext()[KudosContextElement]?.value
        ?: error("KudosContext is absent in coroutineContext")

/**
 * If the current coroutine already carries a [KudosContext], run [block] directly; otherwise generate one with
 * [provider] and run [block] wrapped in [withContext].
 *
 * Suitable for "gateway entry points" or "message consumer entry points" — inherit the context if present, otherwise
 * create one as a fallback.
 *
 * @param T The return type of [block]
 * @param provider Function producing the default context; by default constructs an empty [KudosContext]
 * @param block The suspending logic to execute
 * @return The return value of [block]
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
 * Override the current coroutine's [KudosContext] with the explicit [ctx] and run [block].
 * Suitable for scenarios that need to replace the entire context, such as "simulating another user/tenant".
 *
 * @param T The return type of [block]
 * @param ctx The explicit context
 * @param block The suspending logic to execute
 * @return The return value of [block]
 * @author K
 * @since 1.0.0
 */
suspend inline fun <T> withKudosContext(
    ctx: KudosContext,
    crossinline block: suspend () -> T
): T = withContext(KudosContextElement(ctx)) { block() }

/**
 * Launch a coroutine on an existing [CoroutineScope] while explicitly attaching a [KudosContext].
 * A convenience wrapper equivalent to `scope.launch(extraContext + KudosContextElement(ctx)) { ... }`.
 *
 * @param ctx The [KudosContext] to attach
 * @param context Additional [CoroutineContext] elements; empty by default
 * @param block The coroutine body
 * @return The newly started [kotlinx.coroutines.Job]
 * @author K
 * @since 1.0.0
 */
fun CoroutineScope.launchWithKudos(
    ctx: KudosContext,
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
) = launch(context + KudosContextElement(ctx), block = block)


