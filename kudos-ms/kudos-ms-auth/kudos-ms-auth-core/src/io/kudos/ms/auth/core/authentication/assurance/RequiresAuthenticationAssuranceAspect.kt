package io.kudos.ms.auth.core.authentication.assurance

import io.kudos.ms.auth.common.authentication.annotation.RequiresAuthenticationAssurance
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.aop.support.AopUtils
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/** Enforces [RequiresAuthenticationAssurance] on HTTP-backed method entry. */
@Aspect
open class RequiresAuthenticationAssuranceAspect(
    private val verifier: AuthenticationAssuranceVerifier,
) {

    @Around("@annotation(io.kudos.ms.auth.common.authentication.annotation.RequiresAuthenticationAssurance) || " +
        "@within(io.kudos.ms.auth.common.authentication.annotation.RequiresAuthenticationAssurance)")
    open fun enforce(joinPoint: ProceedingJoinPoint): Any? {
        val signatureMethod = (joinPoint.signature as MethodSignature).method
        val targetClass = joinPoint.target?.javaClass ?: signatureMethod.declaringClass
        val method = AopUtils.getMostSpecificMethod(signatureMethod, targetClass)
        val required = AnnotatedElementUtils.findMergedAnnotation(
            method,
            RequiresAuthenticationAssurance::class.java,
        ) ?: AnnotatedElementUtils.findMergedAnnotation(
            signatureMethod,
            RequiresAuthenticationAssurance::class.java,
        ) ?: AnnotatedElementUtils.findMergedAnnotation(
            targetClass,
            RequiresAuthenticationAssurance::class.java,
        ) ?: return joinPoint.proceed()

        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        val session = request?.getAttribute(AuthenticationSession.REQUEST_ATTRIBUTE) as? AuthenticationSession
        verifier.verify(session, required.acr, required.maxAgeSeconds)
        return joinPoint.proceed()
    }
}
