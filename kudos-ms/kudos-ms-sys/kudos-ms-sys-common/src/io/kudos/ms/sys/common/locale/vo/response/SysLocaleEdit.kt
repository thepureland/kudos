package io.kudos.ms.sys.common.locale.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 语言字典编辑响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysLocaleEdit(

    /** 主键 */
    override val id: String = "",

    /** 语言代码 */
    val code: String = "",

    /** 显示名称 */
    val displayName: String = "",

    /** 英文名称 */
    val englishName: String = "",

    /** 排序号 */
    val sortNo: Int = 0,

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

) : IIdEntity<String>
