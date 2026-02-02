package io.kudos.ms.sys.common.vo.tenantresource

import java.io.Serializable
import io.kudos.base.support.IIdEntity


/**
 * 租户-资源关系缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantResourceCacheItem (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 租户id */
    var tenantId: String? = null,

    /** 资源id */
    var resourceId: String? = null,

    //endregion your codes 1
//region your codes 2
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

    companion object {
        private const val serialVersionUID = 2464879711249952621L
    }

}