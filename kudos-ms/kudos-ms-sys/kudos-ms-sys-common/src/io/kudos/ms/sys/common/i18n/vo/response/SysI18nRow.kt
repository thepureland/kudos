package io.kudos.ms.sys.common.i18n.vo.response
/**
 * 国际化列表查询结果响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nRow (

    /** 主键 */
    val id: String = "",

    /** 语言_地区 */
    val locale: String = "",

    /** 原子服务编码 */
    val atomicServiceCode: String = "",

    /** 国际化类型字典代码 */
    val i18nTypeDictCode: String = "",

    /** 国际化命名空间 */
    val namespace: String = "",

    /** 国际化key */
    val key: String = "",

    /** 国际化值 */
    val value: String = "",

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = true,

)