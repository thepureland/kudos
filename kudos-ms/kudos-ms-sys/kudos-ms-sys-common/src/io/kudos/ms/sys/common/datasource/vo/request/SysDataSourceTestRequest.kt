package io.kudos.ms.sys.common.datasource.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.NotBlank


/**
 * 数据源连通性测试请求
 *
 * @author K
 * @since 1.0.0
 */
data class SysDataSourceTestRequest(

    /** JDBC URL */
    @get:NotBlank
    @get:MaxLength(256)
    val url: String,

    /** 用户名 */
    @get:NotBlank
    @get:MaxLength(32)
    val username: String,

    /** 密码（明文；调用方负责安全传输） */
    val password: String?,

)
