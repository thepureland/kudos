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
    override val id: String,

    /** 名称 */
    val name: String?,

    /** url */
    val url: String?,

    /** 资源类型字典代码 */
    val resourceTypeDictCode: String?,

    /** 父id */
    val parentId: String?,

    /** 在同父节点下的排序号 */
    val orderNum: Int?,

    /** 图标 */
    val icon: String?,

    /** 子系统编码 */
    val subSystemCode: String?,

    /** 备注 */
    val remark: String?,

    /** 是否启用 */
    val active: Boolean?,

    /** 是否内置 */
    val builtIn: Boolean?,

    /** 创建者id */
    val createUserId: String?,

    /** 创建者名称 */
    val createUserName: String?,

    /** 创建时间 */
    val createTime: LocalDateTime?,

    /** 更新者id */
    val updateUserId: String?,

    /** 更新者名称 */
    val updateUserName: String?,

    /** 更新时间 */
    val updateTime: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 8029707342616140104L
    }

}
