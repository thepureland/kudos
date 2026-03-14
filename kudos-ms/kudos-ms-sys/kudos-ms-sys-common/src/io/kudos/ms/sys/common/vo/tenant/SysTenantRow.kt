package io.kudos.ms.sys.common.vo.tenant

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 租户查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysTenantRow (


    /** 主键 */
    override val id: String = "",

    /** 名称 */
    val name: String = "",

    /** 时区 */
    val timezone: String? = null,

    /** 默认语言编码 */
    val defaultLanguageCode: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否启用 */
    val active: Boolean = true,

    /** 是否内置 */
    val builtIn: Boolean = false,

) : IdJsonResult<String>() {


    constructor() : this("")

    /** 以逗号分隔的子系统编码 */
    var subSystemCodes: String = ""


}