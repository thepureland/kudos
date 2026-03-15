package io.kudos.ms.user.common.vo.org

import io.kudos.base.model.contract.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 机构查询记录
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class UserOrgDetail (


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

) : IdJsonResult<String>() {


    constructor() : this("")


}
