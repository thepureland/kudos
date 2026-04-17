package io.kudos.ms.sys.common.cache.vo.request

import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import jakarta.validation.constraints.NotBlank

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
    val name: String ,

    /** 原子服务编码 */
    @get:NotBlank
    @get:MaxLength(32)
    val atomicServiceCode: String ,

    override val strategyDictCode: String ,

    override val writeOnBoot: Boolean ,

    override val writeInTime: Boolean ,

    override val ttl: Int? ,

    override val remark: String? ,

    /** 是否为 Hash 缓存 */
    val hash: Boolean ,

) : ISysCacheFormBase