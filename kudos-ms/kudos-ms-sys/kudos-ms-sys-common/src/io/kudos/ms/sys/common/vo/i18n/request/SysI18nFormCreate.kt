package io.kudos.ms.sys.common.vo.i18n.request

import jakarta.validation.constraints.NotBlank


/**
 * 国际化表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nFormCreate (

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

    /** 备注 */
    val remark: String? = null,

)