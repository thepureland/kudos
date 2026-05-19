package io.kudos.ability.log.audit.common.annotation

import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.log.audit.common.support.DefaultAuditLogDetailDescriptionFormatter
import io.kudos.ability.log.audit.common.support.IAuditLogDetailDescriptionFormatter
import io.kudos.base.enums.impl.YesNotEnum
import kotlin.reflect.KClass


@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class Audit(
    /**
     * 操作类型
     */
    val opType: OperationTypeEnum,
    /**
     * 所属子系统
     */
    val subSysCode: String = "",
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
     *
     * @return
     */
    val descriptionFormatter: KClass<out IAuditLogDetailDescriptionFormatter> = DefaultAuditLogDetailDescriptionFormatter::class,

    /**
     * 业务模型参数在方法签名中的索引。默认 `0`（兼容旧行为：取第一个参数作为 model）。
     *
     * 旧实现 `joinPoint.args[0]` 硬编码——业务方法如果是 `(String tenantId, User user)`，
     * 切面会拿到 tenantId 而非 user，oldBizData 加载和 entityId 提取都会错。
     * 业务侧可以通过 `@Audit(..., modelArgIndex = 1)` 指明 user 是第 1 个位置。
     */
    val modelArgIndex: Int = 0,
)
