package io.kudos.ms.sys.common.vo.i18n


/**
 * 国际化缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nCacheItem (

    //region your codes 1

    /** 语言_地区 */
    var locale: String? = null,

    /** 原子服务编码 */
    var atomicServiceCode: String? = null,

    /** 国际化类型字典代码 */
    var i18nTypeDictCode: String? = null,

    /** 国际化key */
    var key: String? = null,

    /** 国际化值 */
    var value: String? = null,

    //endregion your codes 1
//region your codes 2
) {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}
