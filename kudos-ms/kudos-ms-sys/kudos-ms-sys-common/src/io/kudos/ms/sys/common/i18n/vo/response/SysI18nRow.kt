package io.kudos.ms.sys.common.i18n.vo.response

/**
 * Response VO for i18n list query result.
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nRow (

    /** Primary key */
    val id: String = "",

    /** Language_Region */
    val locale: String = "",

    /** Atomic service code */
    val atomicServiceCode: String = "",

    /** I18n type dictionary code */
    val i18nTypeDictCode: String = "",

    /** I18n namespace */
    val namespace: String = "",

    /** I18n key */
    val key: String = "",

    /** I18n value */
    val value: String = "",

    /** Remark */
    val remark: String? = null,

    /** Whether enabled */
    val active: Boolean = true,

    /** Whether built-in */
    val builtIn: Boolean = true,

)