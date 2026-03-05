package io.kudos.ms.sys.common.vo.cache

import io.kudos.base.support.result.IdJsonResult


/**
 * 缓存查询记录
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheRecord (

    //region your codes 1

    /** 主键 */
    override var id: String = "",

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

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}