package io.kudos.ms.user.common.account.vo.response
import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 机构-用户关系详情响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserOrgUserDetail (

    /** 主键 */
    override val id: String = "",

    /** 机构id */
    val orgId: String? = null,

    /** 用户id */
    val userId: String? = null,

    /** 是否为机构管理员 */
    val orgAdmin: Boolean? = null,

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