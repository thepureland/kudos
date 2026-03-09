package io.kudos.ms.sys.common.vo.tenant

import io.kudos.base.support.IIdEntity
import java.io.Serializable


/**
 * 租户缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantCacheItem (

    /** 主键 */
    override val id: String = "",

    //region your codes 1

    /** 名称 */
    val name: String = "",

    /** 时区 */
    val timezone: String? = null,

    /** 默认语言编码 */
    val defaultLanguageCode: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = false,

    //endregion your codes 1
//region your codes 2
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

    companion object {
        private const val serialVersionUID = 2728865406469746023L
    }

}