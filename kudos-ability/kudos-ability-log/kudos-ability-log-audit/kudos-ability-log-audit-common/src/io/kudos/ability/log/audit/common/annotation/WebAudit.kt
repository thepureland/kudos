package io.kudos.ability.log.audit.common.annotation

import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.log.audit.common.support.DefaultAuditLogDetailDescriptionFormatter
import io.kudos.ability.log.audit.common.support.IAuditLogDetailDescriptionFormatter
import io.kudos.base.enums.impl.YesNotEnum
import kotlin.reflect.KClass

/**
 * Web controller 方法上的审计注解。
 *
 * 与 [Audit] 的区别：[WebAudit] 由
 * [io.kudos.ability.log.audit.common.annotation.WebLogAuditAspect] 处理，
 * 会从 [org.springframework.web.context.request.RequestContextHolder] 拿 HTTP 请求；
 * [Audit] 处理通用方法，从方法参数取业务对象。Multipart 请求会被 WebAudit 切面跳过。
 *
 * @author K
 * @since 1.0.0
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebAudit(
    /**
     * 操作类型
     */
    val opType: OperationTypeEnum,
    /**
     * 所属子系统
     */
    val subsysCode: String = "",
    /**
     * 日志所属模块
     */
    val moduleCode: String,
    /**
     * 操作描述
     */
    val desc: String = "",
    /**
     * 是否忽略表单数据
     * 是	: request提交表单数据将不被存储
     * 否	: request提交表单数据将被存储
     */
    val ignoreForm: YesNotEnum = YesNotEnum.YES,
    /**
     * 操作类型(扩展)
     */
    val opTypeExt: String = "",
    /**
     * 详情日志处理器
     *
     * @return
     */
    val descriptionFormatter: KClass<out IAuditLogDetailDescriptionFormatter> = DefaultAuditLogDetailDescriptionFormatter::class
)
