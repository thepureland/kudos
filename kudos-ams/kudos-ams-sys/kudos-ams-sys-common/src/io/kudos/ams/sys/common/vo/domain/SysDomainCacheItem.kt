package io.kudos.ams.sys.common.vo.domain

import java.io.Serializable


/**
 * 域名缓存项
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysDomainCacheItem : Serializable {
//endregion your codes 1

    //region your codes 2

    companion object {
        private const val serialVersionUID = 4077591078739245970L
    }

    /** 域名 */
    var domain: String? = null

    /** 子系统代码 */
    var subSysDictCode: String? = null

    /** 租户id */
    var tenantId: String? = null

    /** 备注，或其国际化key */
    var remark: String? = null

    /** 是否启用 */
    var active: Boolean? = null

    //endregion your codes 2


}