package io.kudos.ability.comm.websocket.ktor.context

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextElement
import io.kudos.context.core.KudosContextHolder
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Keeps [KudosContextHolder]'s `ThreadLocal` pointing at [value] for as long as the coroutine that
 * carries this element is running — on every thread it happens to run on.
 *
 * ## Why this is needed at all
 *
 * kudos has two context mechanisms that do not know about each other:
 *
 *  - [KudosContextHolder] — an `InheritableThreadLocal`. Practically **every** consumer in the
 *    ability layer reads this one (tenant cache keys, ktorm's `currentDatabase()`, audit logging,
 *    outbound Feign headers).
 *  - [KudosContextElement] — a plain `CoroutineContext.Element`, which propagates correctly across
 *    suspension points but which nothing in production currently reads.
 *
 * A WebSocket handler is a coroutine, so the naive bridge — `KudosContextHolder.set(ctx)` at the top
 * of the message and `clear()` in a `finally` — is broken by design: the coroutine can suspend and
 * resume on a *different* worker thread, after which the `ThreadLocal` set on the original thread is
 * simply not there. It also cannot clean up the threads it hopped onto, which is exactly the pooled
 * thread pollution `kudos-ability-cache/README.md` documents.
 *
 * [ThreadContextElement] is the mechanism built for this: the coroutine machinery calls
 * [updateThreadContext] every time the coroutine is resumed on a thread and [restoreThreadContext]
 * every time it leaves one, so the `ThreadLocal` tracks the coroutine rather than the thread.
 *
 * ## Why it lives here rather than in `kudos-context`
 *
 * Making [KudosContextElement] itself a [ThreadContextElement] would apply this behavior process-wide
 * — including to the 27 production call sites of `KudosContextHolder.get()`, whose "create one if
 * absent and store it" semantics mean a context created *inside* a coroutine would be silently
 * discarded on restore. `XKudosContextHolder` caching a resolved `DataSource` into the context is
 * one such site, and it backs `currentDatabase()` for every ktorm DAO; the failure mode there is a
 * silent performance regression, not an error. `kudos-context/README.md` also records a deliberate
 * decision that the library does not auto-bridge and that entry plugins own it.
 *
 * Scoping the bridge to WebSocket message dispatch gets the benefit with a blast radius of one
 * module. If it proves out, promoting it into `kudos-context` is a separate, informed decision.
 *
 * @param value the context to expose while the coroutine runs
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class KudosContextThreadElement(val value: KudosContext) : ThreadContextElement<KudosContext?> {

    override val key: CoroutineContext.Key<*> get() = Key

    /** @return the context that was bound to this thread before, so [restoreThreadContext] can put it back */
    override fun updateThreadContext(context: CoroutineContext): KudosContext? {
        val previous = KudosContextHolder.getOrNull()
        KudosContextHolder.set(value)
        return previous
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: KudosContext?) {
        // clear() rather than set(null): the holder's setter is non-null, and removing the entry is
        // what its own KDoc prescribes for avoiding leaks on pooled threads.
        if (oldState == null) KudosContextHolder.clear() else KudosContextHolder.set(oldState)
    }

    /** Key for [KudosContextThreadElement] in a [CoroutineContext]. */
    companion object Key : CoroutineContext.Key<KudosContextThreadElement>
}

/**
 * Runs [block] with [ctx] visible through **both** context mechanisms: [KudosContextHolder] for the
 * ThreadLocal-based majority of kudos, and [KudosContextElement] for `currentKudosContext()`.
 *
 * Both are attached because a WebSocket handler may call into either style of code, and attaching
 * only one silently works for half the codebase.
 */
suspend fun <T> withKudosContextBound(ctx: KudosContext, block: suspend () -> T): T =
    withContext(KudosContextElement(ctx) + KudosContextThreadElement(ctx)) { block() }
