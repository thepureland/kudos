package io.kudos.ams.sys.common.vo.dictitemi18n

import java.io.Serializable
import io.kudos.base.support.IIdEntity
import java.time.LocalDateTime


/**
 * 字典项国际化缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemI18nCacheItem (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 语言_地区 */
    var locale: String? = null,

    /** 国际化值 */
    var i18nValue: String? = null,

    /** 字典项id */
    var itemId: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 创建用户 */
    var createUser: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新用户 */
    var updateUser: String? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

    companion object {
        private const val serialVersionUID = 1878598659755844709L
    }

}