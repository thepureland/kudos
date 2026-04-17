package io.kudos.ms.sys.common.dict.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 字典项详情响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysDictItemDetail (

    /** 主键 */
    override val id: String = "",


    /** 字典项代码 */
    val itemCode: String = "",

    /** 字典项名称 */
    val itemName: String = "",

    /** 字典id */
    val dictId: String = "",

    /** 字典项排序 */
    val orderNum: Int? = null,

    /** 父id */
    val parentId: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = true,

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

) : IIdEntity<String>