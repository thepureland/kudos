package io.kudos.ms.sys.common.vo.accessruleip

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * IP访问规则缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpCacheEntry (

    /** 主键 */
    override val id: String,

    /** ip起 */
    val ipStart: Long?,

    /** ip止 */
    val ipEnd: Long?,

    /** ip类型字典代码 */
    val ipTypeDictCode: String?,

    /** 过期时间 */
    val expirationTime: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 6895365638061974342L
    }

}
