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

) : IIdEntity<String>, Serializable {


    constructor() : this("")


    companion object {
        private const val serialVersionUID = 6895365638061974342L
    }

}