package io.kudos.context.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for KudosContextElement and its coroutine helper functions
 *
 * Coverage:
 * - [KudosContextElement.value] holds the context; the companion Key indexes it in a CoroutineContext
 * - [currentKudosContext]: returns the bound context / throws IllegalStateException when absent
 * - [withKudosContextIfAbsent]: inherits an existing context (provider NOT invoked) vs. creates one
 *   via the provider (default and custom) when absent
 * - [withKudosContext]: overrides the current context for the block
 * - [launchWithKudos]: the launched coroutine sees the attached context, including across dispatcher hops
 * - Binary contract of the emitted non-inlined copies, the default-provider lambda singleton, and the
 *   `$default` bridge's default-argument substitution
 *
 * @author K
 * @since 1.0.0
 */
internal class KudosContextElementTest {

    @Test
    fun elementHoldsValueAndIsIndexedByKey() = runBlocking {
        val ctx = KudosContext().apply { traceKey = "t-1" }
        val element = KudosContextElement(ctx)
        assertSame(ctx, element.value)

        withContext(element) {
            val found = kotlinx.coroutines.currentCoroutineContext()[KudosContextElement]
            assertSame(element, found, "the element should be retrievable by its companion Key")
        }
    }

    @Test
    fun currentKudosContextReturnsBoundContext() = runBlocking {
        val ctx = KudosContext().apply { tenantId = "tenant-1" }
        withContext(KudosContextElement(ctx)) {
            assertSame(ctx, currentKudosContext())
        }
    }

    @Test
    fun currentKudosContextThrowsWhenAbsent() {
        val e = assertFailsWith<IllegalStateException> {
            runBlocking { currentKudosContext() }
        }
        assertEquals("KudosContext is absent in coroutineContext", e.message)
    }

    @Test
    fun withKudosContextIfAbsentInheritsExistingAndSkipsProvider() = runBlocking {
        val existing = KudosContext().apply { portalCode = "portal-x" }
        val providerCalled = AtomicBoolean(false)
        val result = withContext(KudosContextElement(existing)) {
            withKudosContextIfAbsent(provider = { providerCalled.set(true); KudosContext() }) {
                currentKudosContext().portalCode
            }
        }
        assertEquals("portal-x", result, "the existing context should be inherited")
        assertFalse(providerCalled.get(), "the provider must not be invoked when a context is already present")
    }

    @Test
    fun withKudosContextIfAbsentCreatesContextViaCustomProvider() = runBlocking {
        val provided = KudosContext().apply { subSystemCode = "sub-y" }
        val seen = withKudosContextIfAbsent(provider = { provided }) {
            currentKudosContext()
        }
        assertSame(provided, seen, "the provider-produced context should be installed for the block")
    }

    @Test
    fun withKudosContextIfAbsentDefaultProviderCreatesEmptyContext() = runBlocking {
        val seen = withKudosContextIfAbsent {
            currentKudosContext()
        }
        assertEquals(null, seen.tenantId, "the default provider should produce an empty context")
    }

    @Test
    fun withKudosContextOverridesCurrentContext() = runBlocking {
        val outer = KudosContext().apply { tenantId = "outer" }
        val inner = KudosContext().apply { tenantId = "inner" }
        withContext(KudosContextElement(outer)) {
            val seenInner = withKudosContext(inner) { currentKudosContext() }
            assertSame(inner, seenInner, "withKudosContext should replace the context inside the block")
            assertNotSame(outer, seenInner)
            assertSame(outer, currentKudosContext(), "the outer context should be restored after the block")
        }
    }

    @Test
    fun withKudosContextReturnsBlockValue() = runBlocking {
        val result = withKudosContext(KudosContext()) { 40 + 2 }
        assertEquals(42, result)
    }

    @Test
    fun launchWithKudosAttachesContextToLaunchedCoroutine() = runBlocking {
        val ctx = KudosContext().apply { traceKey = "launch-trace" }
        val captured = AtomicReference<KudosContext?>()
        val job = launchWithKudos(ctx) {
            captured.set(currentKudosContext())
        }
        job.join()
        assertSame(ctx, captured.get())
    }

    // ============================================================
    // Binary contract: the emitted non-inlined copies of the public inline functions.
    // Kotlin inlines these helpers into call sites, but the compiler also keeps callable copies in
    // KudosContextElementKt for reflective/legacy-binary callers — exercised here the way such a
    // caller would (also the only way coverage tooling can observe the declaration's own bytecode).
    // ============================================================

    private fun emittedCopy(name: String, paramCount: Int) =
        Class.forName("io.kudos.context.core.KudosContextElementKt").declaredMethods
            .first { it.name == name && !it.isSynthetic && it.parameterCount == paramCount }
            .apply { isAccessible = true }

    private suspend fun invokeSuspending(call: (kotlin.coroutines.Continuation<Any?>) -> Any?): Any? =
        kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn { cont -> call(cont) }

    /** A "suspend () -> T" as seen by the JVM: Function1<Continuation<T>, Any?> returning the kudos context. */
    private val contextReadingBlock = { c: kotlin.coroutines.Continuation<Any?> ->
        c.context[KudosContextElement]?.value
    }

    @Test
    fun emittedWithKudosContextCopyInstallsContext() = runBlocking {
        val method = emittedCopy("withKudosContext", 3)
        val ctx = KudosContext().apply { traceKey = "emitted-override" }
        val seen = invokeSuspending { cont -> method.invoke(null, ctx, contextReadingBlock, cont) }
        assertSame(ctx, seen)
    }

    @Test
    fun emittedWithKudosContextIfAbsentCopyUsesProviderWhenAbsent() = runBlocking {
        val method = emittedCopy("withKudosContextIfAbsent", 3)
        val provided = KudosContext().apply { traceKey = "emitted-provided" }
        val provider = { _: kotlin.coroutines.CoroutineContext -> provided }
        val seen = invokeSuspending { cont -> method.invoke(null, provider, contextReadingBlock, cont) }
        assertSame(provided, seen)
    }

    @Test
    fun emittedWithKudosContextIfAbsentCopyInheritsExistingContext() = runBlocking {
        val method = emittedCopy("withKudosContextIfAbsent", 3)
        val existing = KudosContext().apply { traceKey = "emitted-existing" }
        val providerCalled = AtomicBoolean(false)
        val provider = { _: kotlin.coroutines.CoroutineContext ->
            providerCalled.set(true)
            KudosContext()
        }
        val seen = withContext(KudosContextElement(existing)) {
            invokeSuspending { cont -> method.invoke(null, provider, contextReadingBlock, cont) }
        }
        assertSame(existing, seen)
        assertFalse(providerCalled.get(), "the provider must not run when a context already exists")
    }

    // Note: `withKudosContextIfAbsent$default` (and the `$$forInline` twins) are compiler-internal
    // copies kept solely for the inliner — their bytecode hard-codes a null continuation
    // (`aconst_null; invokeinterface Continuation.getContext()`), so their BODY can never complete.
    // The default-provider behavior itself is verified above through the real inline call in
    // withKudosContextIfAbsentDefaultProviderCreatesEmptyContext. The two tests below pin down the
    // parts of those emitted artifacts that ARE executable: the default-provider lambda object the
    // compiler emits for binary callers, and the `$default` bridge's default-argument substitution
    // (which runs before the hard-coded null continuation is dereferenced).

    @Test
    fun emittedDefaultProviderLambdaProducesFreshEmptyContexts() {
        // The compiler materializes the default value `{ KudosContext() }` of the provider parameter
        // as a singleton Function1 for binary (non-inlined) callers. Its contract: every invocation
        // yields a NEW, EMPTY KudosContext.
        val lambdaClass = Class.forName("io.kudos.context.core.KudosContextElementKt\$withKudosContextIfAbsent\$2")
        @Suppress("UNCHECKED_CAST")
        val defaultProvider = lambdaClass.getDeclaredField("INSTANCE").get(null)
                as kotlin.jvm.functions.Function1<kotlin.coroutines.CoroutineContext, KudosContext>

        val first = defaultProvider.invoke(kotlin.coroutines.EmptyCoroutineContext)
        val second = defaultProvider.invoke(kotlin.coroutines.EmptyCoroutineContext)

        assertEquals(null, first.tenantId, "the default provider must produce an empty context")
        assertEquals(null, first.traceKey)
        assertNotSame(first, second, "every invocation must create a fresh context instance")
    }

    @Test
    fun emittedDefaultBridgeSubstitutesDefaultProviderThenRejectsItsNullContinuation() {
        // `withKudosContextIfAbsent$default(provider, block, continuation, mask, marker)` first
        // substitutes the default provider when bit 0 of the mask is set, then runs the inliner-only
        // body — which dereferences its hard-coded null continuation and must fail fast with an NPE
        // (it is not a callable suspend entry point; real binary callers use the 3-arg copy above).
        val bridge = Class.forName("io.kudos.context.core.KudosContextElementKt").declaredMethods
            .first { it.name == "withKudosContextIfAbsent\$default" }
            .apply { isAccessible = true }

        val e = assertFailsWith<java.lang.reflect.InvocationTargetException> {
            bridge.invoke(null, null, contextReadingBlock, null, 1, null)
        }
        assertTrue(
            e.cause is NullPointerException,
            "the inliner-only body must fail on its hard-coded null continuation, got: ${e.cause}"
        )
    }

    @Test
    fun launchWithKudosSurvivesDispatcherHop() = runBlocking {
        // The whole point of the element: the context must survive thread switches inside coroutines
        val ctx = KudosContext().apply { traceKey = "hop" }
        val captured = AtomicReference<String?>()
        val job = launchWithKudos(ctx, context = Dispatchers.Default) {
            withContext(Dispatchers.IO) {
                captured.set(currentKudosContext().traceKey)
            }
        }
        job.join()
        assertEquals("hop", captured.get(), "the context must propagate across dispatcher/thread hops")
        assertTrue(job.isCompleted)
    }
}
