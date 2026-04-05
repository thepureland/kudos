package io.kudos.ms.sys.common.resource.vo.response
import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 资源详情响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceDetail (

    /** 主键 */
    override val id: String = "",


    /** 名称 */
    val name: String = "",

    /** url */
    val url: String? = null,

    /** 资源类型字典代码 */
    val resourceTypeDictCode: String = "",

    /** 父id */
    val parentId: String? = null,

    /** 在同父节点下的排序号 */
    val orderNum: Int? = null,

    /** 图标 */
    val icon: String? = null,

    /** 子系统编码 */
    val subSystemCode: String = "",

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = false,

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

) : IIdEntity<String> {


    /** 所有父项ID */
    var parentIds: List<String>? = null

}