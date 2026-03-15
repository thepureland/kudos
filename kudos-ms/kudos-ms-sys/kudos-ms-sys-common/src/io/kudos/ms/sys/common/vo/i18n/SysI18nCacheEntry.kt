package io.kudos.ms.sys.common.vo.i18n

import io.kudos.base.model.contract.entity.IIdEntity

/**
 * 国际化缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nCacheEntry (

    /** 主键 */
    override val id: String? = null,


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

) : IIdEntity<String?> {


    constructor() : this(null)


}
