package io.kudos.ams.sys.common.vo.cache

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 缓存查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheDetail (

    /** 主键 */
    override var id: String? = null,

    //region your codes 1

    /** 名称 */
    var name: String? = null,

    /** 原子服务编码 */
    var atomicServiceCode: String? = null,

    /** 缓存策略代码 */
    var strategyDictCode: String? = null,

    /** 是否启动时写缓存 */
    var writeOnBoot: Boolean? = null,

    /** 是否及时回写缓存 */
    var writeInTime: Boolean? = null,

    /** 缓存生存时间(秒) */
    var ttl: Int? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    /** 创建用户 */
    var createUser: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新用户 */
    var updateUser: String? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    //endregion your codes 3

}