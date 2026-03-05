package io.kudos.ms.sys.common.vo.cache

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
    override var id: String = "",

    //region your codes 1

    /** 名称 */
    var name: String = "",

    /** 原子服务编码 */
    var atomicServiceCode: String = "",

    /** 缓存策略代码 */
    var strategyDictCode: String = "",

    /** 是否启动时写缓存 */
    var writeOnBoot: Boolean = true,

    /** 是否及时回写缓存 */
    var writeInTime: Boolean = true,

    /** 缓存生存时间(秒) */
    var ttl: Int? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否启用 */
    var active: Boolean = true,

    /** 是否内置 */
    var builtIn: Boolean = true,

    /** 是否为 Hash 缓存 */
    var hash: Boolean = false,

    /** 创建者id */
    var createUserId: String? = null,

    /** 创建者名称 */
    var createUserName: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新者id */
    var updateUserId: String? = null,

    /** 更新者名称 */
    var updateUserName: String? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    //endregion your codes 3

}