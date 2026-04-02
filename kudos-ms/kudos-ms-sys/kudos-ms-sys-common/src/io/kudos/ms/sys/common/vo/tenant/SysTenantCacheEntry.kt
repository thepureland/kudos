package io.kudos.ms.sys.common.vo.tenant

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 租户缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantCacheEntry (

    /** 主键 */
    override val id: String,


    /** 名称 */
    val name: String,

    /** 时区 */
    val timezone: String?,

    /** 默认语言编码 */
    val defaultLanguageCode: String?,

    /** 备注 */
    val remark: String?,

    /** 是否启用 */
    val active: Boolean,

    /** 是否内置 */
    val builtIn: Boolean,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 2728865406469746023L
    }

}
