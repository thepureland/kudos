package io.kudos.ms.user.common.vo.org

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 机构缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class UserOrgCacheEntry (

    /** 主键 */
    override val id: String,

    /** 机构名称 */
    val name: String?,

    /** 机构简称 */
    val shortName: String?,

    /** 租户id */
    val tenantId: String?,

    /** 父机构id */
    val parentId: String?,

    /** 机构类型字典码 */
    val orgTypeDictCode: String?,

    /** 排序号 */
    val sortNum: Int?,

    /** 备注 */
    val remark: String?,

    /** 是否激活 */
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
        private const val serialVersionUID = 1L
    }

}
