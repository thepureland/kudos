package io.kudos.ms.sys.common.vo.cache

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.support.payload.FormPayload
import io.kudos.ms.sys.common.consts.SysConsts
import io.kudos.ms.sys.common.consts.SysDictTypes
import jakarta.validation.constraints.NotBlank


/**
 * 缓存表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheForm (

    /** 主键 */
    override val id: String? = null,

    //region your codes 1

    /** 名称 */
    @get:NotBlank
    val name: String = "",

    /** 原子服务编码 */
    @get:NotBlank
    val atomicServiceCode: String = "",

    /** 缓存策略代码 */
    @get:NotBlank
    @get:DictItemCode(dictType = SysDictTypes.CACHE_STRATEGY, atomicServiceCode = SysConsts.ATOMIC_SERVICE_NAME)
    val strategyDictCode: String = "",

    /** 是否启动时写缓存 */
    val writeOnBoot: Boolean = true,

    /** 是否及时回写缓存 */
    val writeInTime: Boolean = true,

    /** 缓存生存时间(秒) */
    val ttl: Int? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否为 Hash 缓存 */
    val hash: Boolean = false,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String?>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    // endregion your codes 3

}