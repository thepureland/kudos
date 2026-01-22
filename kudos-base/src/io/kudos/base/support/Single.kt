package io.kudos.base.support

/**
 * 一个只包装单个值的轻量容器（类似 Pair 的“单值版本”）。
 *
 * - 提供常用函数式操作：map / flatMap / filter / fold / getOrElse 等。
 *
 * @author K
 * @author AI: ChatGPT
 * @since 1.0.0
 */
class Single<T>(var value: T) {

    operator fun component1(): T = value

    /** 把 value 原地更新 */
    inline fun update(block: (T) -> T): Single<T> {
        value = block(value)
        return this
    }

    /** 只读映射：生成新的 Single */
    inline fun <R> map(transform: (T) -> R): Single<R> =
        Single(transform(value))

    /** 原地映射：直接修改自己 */
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
