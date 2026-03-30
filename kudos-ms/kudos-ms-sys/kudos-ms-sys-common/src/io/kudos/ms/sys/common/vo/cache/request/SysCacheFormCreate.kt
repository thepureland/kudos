package io.kudos.ms.sys.common.vo.cache.request

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import io.kudos.ms.sys.common.consts.SysConsts
import io.kudos.ms.sys.common.consts.SysDictTypes
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.Length

/**
 * 缓存表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysCacheFormCreate (

    /** 名称 */
    @get:NotBlank
    @get:Matches(RegExpEnum.VAR_NAME)
    @get:MaxLength(64)
    val name: String = "",

    /** 原子服务编码 */
    @get:NotBlank
    @get:MaxLength(32)
    val atomicServiceCode: String = "",

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

    /** 是否为 Hash 缓存 */
    val hash: Boolean = false,

)