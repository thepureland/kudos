package io.kudos.ms.sys.common.vo.resource

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 资源缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceCacheEntry (

    /** 主键 */
    override val id: String = "",


    /** 名称 */
    val name: String? = null,

    /** url */
    val url: String? = null,

    /** 资源类型字典代码 */
    val resourceTypeDictCode: String? = null,

    /** 父id */
    val parentId: String? = null,

    /** 在同父节点下的排序号 */
    val orderNum: Int? = null,

    /** 图标 */
    val icon: String? = null,

    /** 子系统编码 */
    val subSystemCode: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

    /** 创建者id */
    val createUserId: String? = null,

    /** 创建者名称 */
    val createUserName: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新者id */
    val updateUserId: String? = null,

    /** 更新者名称 */
    val updateUserName: String? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>, Serializable {


    constructor() : this("")


    companion object {
        private const val serialVersionUID = 8029707342616140104L
    }

}