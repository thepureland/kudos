package io.kudos.base.enums.impl

import io.kudos.base.enums.ienums.IDictEnum

/**
 * Sex enum.
 *
 * @author K
 * @since 1.0.0
 */
enum class SexEnum(override val code: String, override val displayText: String) : IDictEnum {

    FEMALE("0", "Female"),
    MALE("1", "Male"),
    SECRET("9", "Undisclosed");

}