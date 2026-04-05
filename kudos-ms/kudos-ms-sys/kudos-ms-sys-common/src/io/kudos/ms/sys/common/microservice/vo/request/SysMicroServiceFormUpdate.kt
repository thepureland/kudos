package io.kudos.ms.sys.common.microservice.vo.request
import io.kudos.base.bean.validation.constraint.annotations.FixedLength
import io.kudos.base.model.contract.entity.IIdEntity
import jakarta.validation.constraints.NotBlank


/**
 * 微服务表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class SysMicroServiceFormUpdate (

    @get:NotBlank
    override val code: String,

    override val name: String,

    override val context: String,

    override val atomicService: Boolean = true,

    override val parentCode: String?,

    override val remark: String?,

) : ISysMicroServiceFormBase, IIdEntity<String> {

    override val id: String
        get() = code

}
