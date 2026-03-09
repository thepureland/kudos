package io.kudos.ms.sys.common.vo.i18n

import io.kudos.base.support.payload.FormPayload
import jakarta.validation.constraints.NotBlank


/**
 * 国际化表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nPayload (

    /** 主键 */
    override val id: String = "",

    //region your codes 1

    /** 语言_地区 */
    @get:NotBlank
    val locale: String = "",

    /** 原子服务编码 */
    @get:NotBlank
    val atomicServiceCode: String = "",

    /** 国际化类型字典代码 */
    @get:NotBlank
    val i18nTypeDictCode: String = "",

    /** 国际化命名空间 */
    @get:NotBlank
    val namespace: String = "",

    /** 国际化key */
    @get:NotBlank
    val key: String = "",

    /** 国际化值 */
    @get:NotBlank
    val value: String = "",

    /** 是否启用 */
    val active: Boolean = true,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}
