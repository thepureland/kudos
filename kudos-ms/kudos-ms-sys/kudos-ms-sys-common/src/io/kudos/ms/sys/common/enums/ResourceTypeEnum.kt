package io.kudos.ms.sys.common.enums

import io.kudos.base.enums.ienums.IDictEnum

/**
 * 资源类型枚举
 *
 * @author K
 * @since 1.0.0
 */
enum class ResourceTypeEnum(override val code: String, override val trans: String): IDictEnum {

    MENU("1", "菜单"),
    FUNCTION("2", "功能"),
    ACTION("3", "请求")

}