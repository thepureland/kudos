package io.kudos.ms.sys.common.vo.resource

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 资源查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysResourceDetail (

    /** 主键 */
    override val id: String = "",

    //region your codes 1

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

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    //endregion your codes 3

}