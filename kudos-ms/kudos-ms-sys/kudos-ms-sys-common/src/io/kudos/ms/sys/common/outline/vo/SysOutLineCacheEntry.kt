package io.kudos.ms.sys.common.outline.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 出网白名单缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysOutLineCacheEntry(

    /** 主键 */
    override val id: String,

    /** 名称 */
    val name: String,

    /** 主机名或通配符 */
    val host: String,

    /** 端口；`null` 表示任意端口 */
    val port: Int?,

    /** 协议(http/https/tcp/any) */
    val protocol: String,

    /** 系统编码 */
    val systemCode: String,

    /** 租户id；`null` 表示平台级 */
    val tenantId: String?,

    /** 备注 */
    val remark: String?,

    /** 是否启用 */
    val active: Boolean,

    /** 是否内置 */
    val builtIn: Boolean,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 5184729285406513961L
    }

}
