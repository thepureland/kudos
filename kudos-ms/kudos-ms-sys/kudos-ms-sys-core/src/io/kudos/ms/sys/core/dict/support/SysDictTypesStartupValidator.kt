package io.kudos.ms.sys.core.dict.support

import io.kudos.base.logger.LogFactory
import io.kudos.ms.sys.common.platform.consts.SysConsts
import io.kudos.ms.sys.common.platform.consts.SysDictTypes
import io.kudos.ms.sys.core.dict.dao.SysDictDao
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.lang.reflect.Modifier

/**
 * Startup validator: confirms that every dictionary type constant declared in [SysDictTypes] has a corresponding
 * active record in the `sys_dict` table (`atomic_service_code = "sys"` and `active = true`).
 *
 * Historical issue: [SysDictTypes] is `const val`, while `sys_dict.dict_type` is another "source of truth". When the
 * two drift, `@DictItemCode(dictType = SysDictTypes.IP_TYPE)` will **always fail validation** with no compile-time
 * hint. This validator runs once at [ApplicationReadyEvent] (after Flyway / cache loading).
 *
 * Configuration:
 * - `kudos.ms.sys.startup.dict-types-validation.enabled` — default `true`; turn off to skip the entire validation.
 * - `kudos.ms.sys.startup.dict-types-validation.fail-on-missing` — default `false`.
 *   When `true`, throws [IllegalStateException] if anything is missing, **causing application startup to fail**;
 *   suitable for CI / staging environments.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Component
open class SysDictTypesStartupValidator(
    private val sysDictDao: SysDictDao,
    @param:Value("\${kudos.ms.sys.startup.dict-types-validation.enabled:true}") private val enabled: Boolean,
    @param:Value("\${kudos.ms.sys.startup.dict-types-validation.fail-on-missing:false}") private val failOnMissing: Boolean,
) {

    private val log = LogFactory.getLog(this::class)

    // kotlin-spring (allopen) by default treats all members of @Component classes as open; open properties with a
    // `private set` are illegal (Kotlin errors out with "Private setters for open properties are prohibited"). Mark
    // this property `final` explicitly to disable its openness and avoid being affected by allopen.
    @Volatile
    final var lastResult: ValidationResult? = null
        private set

    @EventListener(ApplicationReadyEvent::class)
    open fun onApplicationReady() {
        if (!enabled) {
            log.info("SysDictTypes startup validation is disabled (kudos.ms.sys.startup.dict-types-validation.enabled=false)")
            return
        }
        val result = validate(SysConsts.ATOMIC_SERVICE_NAME)
        lastResult = result
        if (result.missing.isNotEmpty()) {
            val msg = "SysDictTypes startup validation failed: the following dictionary types have no active record in the sys_dict table (atomicServiceCode=${SysConsts.ATOMIC_SERVICE_NAME}): " +
                    "${result.missing}. Affected @DictItemCode validations will always fail; check the Flyway seed data or code constants."
            if (failOnMissing) {
                throw IllegalStateException(msg)
            } else {
                log.error(msg)
            }
        }
        if (result.extras.isNotEmpty()) {
            log.warn(
                "sys_dict contains dict_type values with atomicServiceCode={0} that are not declared in SysDictTypes: {1}. " +
                        "Possibly historical leftovers or newly added constants not yet synced.",
                SysConsts.ATOMIC_SERVICE_NAME, result.extras,
            )
        }
        if (result.missing.isEmpty() && result.extras.isEmpty()) {
            log.info("SysDictTypes startup validation passed: {0} dict_type entries are consistent with the database.", result.declared.size)
        }
    }

    /**
     * Performs a single validation and returns the result (no logging side effects).
     *
     * @param atomicServiceCode comparison scope; fixed to [SysConsts.ATOMIC_SERVICE_NAME] in production, tests can
     *   pass an independent code to avoid conflicts with shared `sys_dict` seed data.
     */
    open fun validate(atomicServiceCode: String = SysConsts.ATOMIC_SERVICE_NAME): ValidationResult {
        val declared = readDeclaredDictTypes()
        val dbTypes = sysDictDao.searchDictsByAtomicServiceCode(atomicServiceCode)
            .map { it.dictType }
            .toSet()
        val missing = declared.filterNot { it in dbTypes }.sorted()
        val extras = dbTypes.filterNot { it in declared }.sorted()
        return ValidationResult(declared = declared, missing = missing, extras = extras)
    }

    /**
     * Reflectively reads all String-typed `const val`s in [SysDictTypes].
     * A Kotlin `object`'s `const val` becomes a public static final field on the JVM; reading via Java reflection is
     * the most stable approach, avoiding edge-case behavior of `KClass.memberProperties` for const fields.
     */
    private fun readDeclaredDictTypes(): Set<String> =
        SysDictTypes::class.java.declaredFields
            .filter { f ->
                Modifier.isStatic(f.modifiers) && Modifier.isFinal(f.modifiers) && f.type == String::class.java
            }
            .mapNotNull { f ->
                f.isAccessible = true
                (f.get(null) as? String)?.takeIf { it.isNotBlank() }
            }
            .toSet()

    data class ValidationResult(
        /** All dictionary types declared as code constants */
        val declared: Set<String>,
        /** Dictionary types declared but not present in the database (active=true) — **must be fixed** */
        val missing: List<String>,
        /** Dictionary types present in the database but not declared as code constants — warning, possibly leftovers */
        val extras: List<String>,
    ) {
        val isOk: Boolean get() = missing.isEmpty()
    }
}
