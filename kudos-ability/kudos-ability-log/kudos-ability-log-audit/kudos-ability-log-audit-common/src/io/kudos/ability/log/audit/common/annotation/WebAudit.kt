package io.kudos.ability.log.audit.common.annotation

import io.kudos.ability.log.audit.common.enums.OperationTypeEnum
import io.kudos.ability.log.audit.common.support.DefaultAuditLogDetailDescriptionFormatter
import io.kudos.ability.log.audit.common.support.IAuditLogDetailDescriptionFormatter
import io.kudos.base.enums.impl.YesNotEnum
import kotlin.reflect.KClass

/**
 * Audit annotation for web controller methods.
 *
 * Difference from [Audit]: [WebAudit] is handled by
 * [io.kudos.ability.log.audit.common.annotation.WebLogAuditAspect], which obtains the HTTP request from
 * [org.springframework.web.context.request.RequestContextHolder]; [Audit] handles general methods and takes the
 * business object from the method parameters. Multipart requests are skipped by the WebAudit aspect.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebAudit(
    /**
     * Operation type
     */
    val opType: OperationTypeEnum,
    /**
     * Owning sub-system
     */
    val subsysCode: String = "",
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
     * Detail-log handler
     *
     * @return
     */
    val descriptionFormatter: KClass<out IAuditLogDetailDescriptionFormatter> = DefaultAuditLogDetailDescriptionFormatter::class
)
