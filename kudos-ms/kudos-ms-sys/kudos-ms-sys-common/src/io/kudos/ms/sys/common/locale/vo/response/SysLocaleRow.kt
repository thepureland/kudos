package io.kudos.ms.sys.common.locale.vo.response

import java.time.LocalDateTime


/**
 * 语言字典列表查询结果响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysLocaleRow(

    /** 主键 */
    val id: String = "",

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

    /** 创建时间 */
    val createTime: LocalDateTime? = null,
)
