package io.kudos.ms.sys.common.resource.enums

import io.kudos.base.enums.ienums.IDictEnum

/**
 * Resource type enum.
 *
 * @author K
 * @since 1.0.0
 */
enum class ResourceTypeEnum(override val code: String, override val displayText: String): IDictEnum {

    MENU("1", "Menu"),
    FUNCTION("2", "Function"),
    ACTION("3", "Action")

}