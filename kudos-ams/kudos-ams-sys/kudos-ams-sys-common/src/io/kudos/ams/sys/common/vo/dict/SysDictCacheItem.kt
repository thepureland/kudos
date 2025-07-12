package io.kudos.ams.sys.common.vo.dict

import io.kudos.base.support.IIdEntity
import java.io.Serializable


/**
 * 字典缓存项
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysDictCacheItem : IIdEntity<String>, Serializable {
//endregion your codes 1

    //region your codes 2

    companion object {
        private const val serialVersionUID = 3917312932640748888L
    }

    /** 主键 */
    override var id: String? = null

    /** 模块 */
    var module: String? = null

    /** 字典类型 */
    var dictType: String? = null

    /** 字典名称，或其国际化key */
    var dictName: String? = null

    /** 是否内置 */
    var builtIn: Boolean? = null

    /** 备注 */
    var remark: String? = null

    //endregion your codes 2


}