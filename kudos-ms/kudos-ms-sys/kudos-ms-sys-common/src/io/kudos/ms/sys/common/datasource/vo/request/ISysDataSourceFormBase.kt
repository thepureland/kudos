package io.kudos.ms.sys.common.datasource.vo.request

import io.kudos.base.bean.validation.constraint.annotations.Compare
import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import io.kudos.base.support.logic.LogicOperatorEnum
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

/**
 * Data source form base fields (shared by create / update)
 *
 * @author K
 * @since 1.0.0
 */
interface ISysDataSourceFormBase {

    /** Name */
    @get:NotBlank
    @get:MaxLength(64)
    @get:Matches(RegExpEnum.VAR_NAME)
    val name: String

    /** Subsystem code */
    @get:NotBlank
    @get:Matches(RegExpEnum.VAR_NAME)
    val subSystemCode: String

    /** Microservice code */
    @get:NotBlank
    @get:Matches(RegExpEnum.VAR_NAME)
    val microServiceCode: String

    /** Tenant id */
    val tenantId: String?

    /** URL */
    @get:NotBlank
    @get:Matches(RegExpEnum.JDBC_URL)
    val url: String

    /** Username */
    @get:NotBlank
    @get:Matches(RegExpEnum.VAR_NAME)
    @get:MaxLength(64)
    val username: String

    /** Password */
    @get:MaxLength(64)
    val password: String?

    /** Initial connection count. Initialization occurs on explicit init() call or on the first getConnection() */
    @get:Positive
    val initialSize: Int?

    /** Maximum connection count; must not be less than the initial connection count */
    @get:Positive
    @get:Compare(anotherProperty = "initialSize", logic = LogicOperatorEnum.GE, message = "sys.valid-msg.dataSource.Compare::maxActive")
    val maxActive: Int?

    /** Maximum idle connections; must not exceed the maximum connection count */
    @get:Positive
    @get:Compare(anotherProperty = "maxActive", logic = LogicOperatorEnum.LE, message = "sys.valid-msg.dataSource.Compare::maxIdle")
    val maxIdle: Int?

    /** Minimum idle connections. Minimum number of idle connections to maintain; must not exceed the maximum connection count */
    @get:Positive
    @get:Compare(anotherProperty = "maxActive", logic = LogicOperatorEnum.LE, message = "sys.valid-msg.dataSource.Compare::minIdle")
    val minIdle: Int?

    /** Maximum borrow duration (ms). If a client borrows a connection from the pool and does not return it before the timeout, the pool will throw an exception */
    @get:Positive
    val maxWait: Int?

    /** Connection lifetime (ms). Once the lifetime (relative to initialization time) is exceeded, the pool will remove the connection on borrow or return */
    @get:Positive
    val maxAge: Int?

    /** Remark */
    @get:MaxLength(128)
    val remark: String?
}
