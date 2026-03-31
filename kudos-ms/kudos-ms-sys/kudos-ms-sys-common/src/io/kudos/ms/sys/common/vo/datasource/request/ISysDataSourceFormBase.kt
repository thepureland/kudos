package io.kudos.ms.sys.common.vo.datasource.request

import io.kudos.base.bean.validation.constraint.annotations.Compare
import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.bean.validation.support.RegExpEnum
import io.kudos.base.support.logic.LogicOperatorEnum
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

/**
 * 数据源表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface ISysDataSourceFormBase {

    /** 名称 */
    @get:NotBlank
    @get:MaxLength(64)
    @get:Matches(RegExpEnum.VAR_NAME)
    val name: String

    /** 子系统编码 */
    @get:NotBlank
    @get:Matches(RegExpEnum.VAR_NAME)
    val subSystemCode: String

    /** 微服务编码 */
    @get:NotBlank
    @get:Matches(RegExpEnum.VAR_NAME)
    val microServiceCode: String

    /** 租户id */
    val tenantId: String?

    /** url */
    @get:NotBlank
    @get:Matches(RegExpEnum.JDBC_URL)
    val url: String

    /** 用户名 */
    @get:NotBlank
    @get:Matches(RegExpEnum.VAR_NAME)
    @get:MaxLength(64)
    val username: String

    /** 密码 */
    @get:MaxLength(64)
    val password: String?

    /** 初始连接数。初始化发生在显示调用init方法，或者第一次getConnection时 */
    @get:Positive
    val initialSize: Int?

    /** 最大连接数，不得小于初始连接数 */
    @get:Positive
    @get:Compare(anotherProperty = "initialSize", logic = LogicOperatorEnum.GE, message = "sys.valid-msg.dataSource.Compare::maxActive")
    val maxActive: Int?

    /** 最大空闲连接数，不得大于最大连接数 */
    @get:Positive
    @get:Compare(anotherProperty = "maxActive", logic = LogicOperatorEnum.LE, message = "sys.valid-msg.dataSource.Compare::maxIdle")
    val maxIdle: Int?

    /** 最小空闲连接数。至少维持多少个空闲连接，不得大于最大连接数 */
    @get:Positive
    @get:Compare(anotherProperty = "maxActive", logic = LogicOperatorEnum.LE, message = "sys.valid-msg.dataSource.Compare::minIdle")
    val minIdle: Int?

    /** 出借最长期限(毫秒)。客户端从连接池获取（借出）一个连接后，超时没有归还（return），则连接池会抛出异常 */
    @get:Positive
    val maxWait: Int?

    /** 连接寿命(毫秒)。超时(相对于初始化时间)连接池将在出借或归还时删除这个连接 */
    @get:Positive
    val maxAge: Int?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
