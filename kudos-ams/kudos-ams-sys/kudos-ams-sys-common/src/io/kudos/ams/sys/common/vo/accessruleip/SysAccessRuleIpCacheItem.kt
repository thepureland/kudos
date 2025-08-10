package io.kudos.ams.sys.common.vo.accessruleip

import java.io.Serializable
import io.kudos.base.support.IIdEntity
import java.time.LocalDateTime


/**
 * ip访问规则缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysAccessRuleIpCacheItem (


    //region your codes 1

    /** 主键 */
    override var id: String? = null,

    /** ip起 */
    var ipStart: Long? = null,

    /** ip止 */
    var ipEnd: Long? = null,

    /** ip类型字典代码 */
    var ipTypeDictCode: String? = null,

    /** 过期时间 */
    var expirationTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IIdEntity<String>, Serializable {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

    companion object {
        private const val serialVersionUID = 6895365638061974342L
    }

}