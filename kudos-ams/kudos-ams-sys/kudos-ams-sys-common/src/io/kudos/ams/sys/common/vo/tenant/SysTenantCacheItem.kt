package io.kudos.ams.sys.common.vo.tenant

import io.kudos.base.support.IIdEntity
import java.io.Serializable


/**
 * 租户缓存项
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysTenantCacheItem : IIdEntity<String>,Serializable {
//endregion your codes 1

    //region your codes 2

    companion object {
        private const val serialVersionUID = 1207475750020689611L
    }

    /** 主键 */
    override var id: String? = null

    /** 名称 */
    var name: String? = null

    /** 备注，或其国际化key */
    var remark: String? = null

    /** 是否内置 */
    var builtIn: Boolean? = null

    //endregion your codes 2


}