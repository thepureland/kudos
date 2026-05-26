package io.kudos.ms.sys.common.cache.vo.request

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.ms.sys.common.platform.consts.SysConsts
import io.kudos.ms.sys.common.platform.consts.SysDictTypes
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

/**
 * Cache form base fields (shared by create / update)
 *
 * @author K
 * @since 1.0.0
 */
interface ISysCacheFormBase {

    /** Cache strategy code */
    @get:NotBlank
    @get:DictItemCode(dictType = SysDictTypes.CACHE_STRATEGY, atomicServiceCode = SysConsts.ATOMIC_SERVICE_NAME)
    @get:MaxLength(16)
    val strategyDictCode: String

    /** Whether to write cache on startup */
    val writeOnBoot: Boolean

    /** Whether to write back cache in real time */
    val writeInTime: Boolean

    /** Cache time-to-live (seconds) */
    @get:Positive
    val ttl: Int?

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
