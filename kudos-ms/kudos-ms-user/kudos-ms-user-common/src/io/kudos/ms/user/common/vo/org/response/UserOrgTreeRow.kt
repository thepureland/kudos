package io.kudos.ms.user.common.vo.org.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 机构树列表查询结果响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserOrgTreeRow (

    /** 主键 */
    override val id: String = "",

    /** 机构名称 */
    val name: String? = null,

    /** 机构简称 */
    val shortName: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 父机构id */
    val parentId: String? = null,

    /** 机构类型字典码 */
    val orgTypeDictCode: String? = null,

    /** 排序号 */
    val sortNum: Int? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否激活 */
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

    /** 子机构列表 */
    var children: MutableList<UserOrgTreeRow>? = null,

) : IIdEntity<String>
