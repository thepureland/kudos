package io.kudos.base.query.sort

import java.util.*

/**
 * 排序方向
 *
 * @author K
 * @since 1.0.0
 */
enum class DirectionEnum {

    ASC,
    DESC;

    companion object Companion {

        fun fromString(value: String): DirectionEnum {
            return try {
                valueOf(value.uppercase(Locale.US))
            } catch (_: IllegalArgumentException) {
                val msg = "非法排序值${value}！取值必须为 'desc' 或 'asc' (大小写不敏感)。"
                error(msg)
            }
        }

    }

}