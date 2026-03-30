package io.kudos.ms.sys.common.vo.cache.request

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.ms.sys.common.consts.SysConsts
import io.kudos.ms.sys.common.consts.SysDictTypes
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

/**
 * 缓存表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheFormUpdate (

    /** 主键 */
    override val id: String? = null,

    /** 缓存策略代码 */
    @get:NotBlank
    @get:DictItemCode(dictType = SysDictTypes.CACHE_STRATEGY, atomicServiceCode = SysConsts.ATOMIC_SERVICE_NAME)
    @get:MaxLength(16)
    val strategyDictCode: String = "",

    /** 是否启动时写缓存 */
    val writeOnBoot: Boolean = true,

    /** 是否及时回写缓存 */
    val writeInTime: Boolean = true,

    /** 缓存生存时间(秒) */
    @get:Positive
    val ttl: Int? = null,

    /** 备注 */
    @get:MaxLength(128)
    val remark: String? = null,

) : IIdEntity<String?>