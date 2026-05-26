package io.kudos.ability.log.audit.common.annotation

import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.log.audit.common.support.DefaultAuditLogDetailDescriptionFormatter
import io.kudos.ability.log.audit.common.support.IAuditLogDetailDescriptionFormatter
import io.kudos.base.enums.impl.YesNotEnum
import kotlin.reflect.KClass


/**
 * Audit-log annotation.
 *
 * Annotates a business method / property getter/setter with the operation type, owning module and description;
 * [io.kudos.ability.log.audit.common.annotation.LogAuditAspect] intercepts around the method execution and persists
 * an audit log.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class Audit(
    /**
     * Operation type
     */
    val opType: OperationTypeEnum,
    /**
     * Owning sub-system
     */
    val subSysCode: String = "",
    /**
     * Module to which the log belongs
     */
    val moduleCode: String,
    /**
     * Operation description
     */
    val desc: String = "",
    /**
     * Whether to ignore form data.
     * YES: form data submitted by the request will not be stored
     * NO : form data submitted by the request will be stored
     */
    val ignoreForm: YesNotEnum = YesNotEnum.YES,
    /**
     * Operation type (extension)
     */
    val opTypeExt: String = "",
    /**
     * Audit-detail description formatter; determines the final rendering of detail.description.
     * Defaults to [DefaultAuditLogDetailDescriptionFormatter]; the business side can customize by implementing
     * [IAuditLogDetailDescriptionFormatter] and plugging it in here.
     */
    val descriptionFormatter: KClass<out IAuditLogDetailDescriptionFormatter> = DefaultAuditLogDetailDescriptionFormatter::class,

    /**
     * Index of the business-model argument in the method signature. Default `0` (compatible with the old behavior:
     * take the first parameter as the model).
     *
     * The old implementation hard-coded `joinPoint.args[0]` — if the business method is `(String tenantId, User
     * user)`, the aspect would pick tenantId rather than user, and both oldBizData loading and entityId extraction
     * would be wrong. The business side can pass `@Audit(..., modelArgIndex = 1)` to indicate user is at position 1.
     */
    val modelArgIndex: Int = 0,
)
