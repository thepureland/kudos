package io.kudos.ability.security.enforcement.aop

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.ability.security.enforcement.init.properties.EnforcementProperties
import io.kudos.ability.security.enforcement.port.IAuthzDecisionProvider
import io.kudos.base.logger.LogFactory
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature


/**
 * Enforces [RequiresPermission] on method entry.
 *
 * The method annotation is looked up first and the declaring class second, so a class-level
 * requirement acts as a default that any method may tighten.
 *
 * Shadow mode is honoured here exactly as in the URL filter — an adoption that is half enforced and
 * half observed would be worse than either.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Aspect
open class RequiresPermissionAspect(
    private val decisionProvider: IAuthzDecisionProvider,
    private val properties: EnforcementProperties,
) {

    private val log = LogFactory.getLog(this::class)

    @Around("@annotation(io.kudos.ability.security.enforcement.annotation.RequiresPermission) || " +
        "@within(io.kudos.ability.security.enforcement.annotation.RequiresPermission)")
    open fun enforce(joinPoint: ProceedingJoinPoint): Any? {
        val method = (joinPoint.signature as MethodSignature).method
        val required = method.getAnnotation(RequiresPermission::class.java)
            ?: method.declaringClass.getAnnotation(RequiresPermission::class.java)
            ?: return joinPoint.proceed()

        val code = required.value
        if (!decisionProvider.hasSubject()) {
            return refuseOrProceed(joinPoint, "no authenticated subject for ${method.name}, which requires ${code}")
        }
        if (!decisionProvider.isPermitted(code)) {
            return refuseOrProceed(joinPoint, "subject lacks ${code} required by ${method.declaringClass.simpleName}.${method.name}")
        }
        return joinPoint.proceed()
    }

    private fun refuseOrProceed(joinPoint: ProceedingJoinPoint, reason: String): Any? {
        if (properties.shadowMode) {
            log.warn("[permission-enforcement][shadow] would deny: ${reason}")
            return joinPoint.proceed()
        }
        throw PermissionDeniedException(reason)
    }
}


/**
 * Thrown when a [RequiresPermission] check fails outside an HTTP exchange, where there is no status
 * code to write. Applications map it to their own error contract.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class PermissionDeniedException(message: String) : RuntimeException(message)
