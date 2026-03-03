package io.kudos.ms.sys.common.vo.cache

import io.kudos.base.bean.validation.constraint.annotations.DictCode
import io.kudos.base.support.payload.FormPayload
import io.kudos.ms.sys.common.consts.SysConsts
import io.kudos.ms.sys.common.consts.SysDictTypes
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull


/**
 * 缓存表单载体
 *
 * @author K
 * @since 1.0.0
 */
data class SysCachePayload (

    /** 主键 */
    override var id: String = "",

    //region your codes 1

    /** 名称 */
    @get:NotBlank(message = "名称不能为空！")
    var name: String? = null,

    /** 原子服务编码 */
    @get:NotBlank(message = "原子服务不能为空！")
    var atomicServiceCode: String? = null,

    /** 缓存策略代码 */
    @get:NotBlank(message = "缓存策略不能为空！")
    @get:DictCode(dictType = SysDictTypes.CACHE_STRATEGY, atomicServiceCode = SysConsts.ATOMIC_SERVICE_NAME, message = "缓存策略非法！")
    var strategyDictCode: String? = null,

    /** 是否启动时写缓存 */
    var writeOnBoot: Boolean? = null,

    /** 是否及时回写缓存 */
    var writeInTime: Boolean? = null,

    /** 缓存生存时间(秒) */
    var ttl: Int? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否为 Hash 缓存 */
    @get:NotNull(message = "必须指明是否为Hash缓存！")
    var hash: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : FormPayload<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    // endregion your codes 3

}