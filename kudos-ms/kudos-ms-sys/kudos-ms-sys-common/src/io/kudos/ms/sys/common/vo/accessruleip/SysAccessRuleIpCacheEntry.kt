package io.kudos.ms.sys.common.vo.accessruleip

import io.kudos.base.support.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * ip访问规则缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpCacheEntry (


    //region your codes 1

    /** 主键 */
    override val id: String = "",

    /** ip起 */
    val ipStart: Long? = null,

    /** ip止 */
    val ipEnd: Long? = null,

    /** ip类型字典代码 */
    val ipTypeDictCode: String? = null,

    /** 过期时间 */
    val expirationTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

    companion object {
        private const val serialVersionUID = 6895365638061974342L
    }

}