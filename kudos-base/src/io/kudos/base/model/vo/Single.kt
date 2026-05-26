package io.kudos.base.model.vo

/**
 * A lightweight container that wraps a single value (like a "single-value version" of Pair).
 *
 * - Provides common functional operations: map / flatMap / filter / fold / getOrElse, etc.
 *
 * @author K
 * @author AI: ChatGPT
 * @since 1.0.0
 */
class Single<T>(var value: T) {

    operator fun component1(): T = value

    /** Update value in place */
    inline fun update(block: (T) -> T): Single<T> {
        value = block(value)
        return this
    }

    /** Read-only map: produces a new Single */
    inline fun <R> map(transform: (T) -> R): Single<R> =
        Single(transform(value))

    /** In-place map: modifies this instance directly */
    inline fun mapInPlace(transform: (T) -> T): Single<T> {
        value = transform(value)
        return this
    }

    override fun toString(): String = "Single(value=$value)"

    companion object {
        fun <T> of(value: T) = Single(value)
    }
}

fun <T> T.singleMutable(): Single<T> = Single(this)
