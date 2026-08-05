package io.kudos.ms.msg.core.support

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

/**
 * Kotlin-friendly Mockito matchers.
 *
 * Plain Mockito matchers (`any()`, `eq()`, `capture()`) return `null` for reference types, which
 * Kotlin rejects with `NullPointerException: <matcher>(...) must not be null` whenever the
 * stubbed/verified method parameter is a non-null Kotlin type. These helpers register the matcher and
 * then return the `null` through an unchecked cast to the erased type parameter, so no null assertion
 * is emitted at the call site while the matcher stack stays correct. Mirrors the repo's existing
 * `anyNonNull` / `captureNonNull` test helpers.
 *
 * @author K
 * @since 1.0.0
 */
object MockitoKt {

    /** Non-null `any()` for a known class. */
    @Suppress("UNCHECKED_CAST")
    fun <T> anyNN(clazz: Class<T>): T = Mockito.any(clazz) as T

    /** Non-null `eq(value)`. */
    @Suppress("UNCHECKED_CAST")
    fun <T> eqNN(value: T): T = ArgumentMatchers.eq(value) as T

    /** Non-null captor capture. */
    @Suppress("UNCHECKED_CAST")
    fun <T> captureNN(captor: ArgumentCaptor<T>): T = captor.capture() as T
}
