package io.kudos.base.query.sort

import java.util.Locale

/**
 * Sort direction.
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
                val msg = "Illegal sort value ${value}! Value must be 'desc' or 'asc' (case insensitive)."
                error(msg)
            }
        }

    }

}