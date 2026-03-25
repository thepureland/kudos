package io.kudos.base.enums.ienums

/**
 * 字典枚举的接口
 *
 * @author K
 * @since 1.0.0
 */
interface IDictEnum {

    /**
     * 代码
     */
    val code: String

    /**
     * 展示文本（可为描述文本或国际化key）
     */
    val displayText: String

}
