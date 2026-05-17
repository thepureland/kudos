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
 * 启动期校验：确认 [SysDictTypes] 中声明的每个字典类型常量都在 `sys_dict` 表中有对应的启用记录
 *（`atomic_service_code = "sys"` 且 `active = true`）。
 *
 * 历史问题：[SysDictTypes] 是 `const val`，而 `sys_dict.dict_type` 是另一份「真相」。两边漂移时
 * `@DictItemCode(dictType = SysDictTypes.IP_TYPE)` 会**永远校验失败**而无任何编译期提示。
 * 本校验在 [ApplicationReadyEvent] 时（Flyway / 缓存加载之后）扫一次。
 *
 * 配置：
 * - `kudos.ms.sys.startup.dict-types-validation.enabled` — 默认 `true`；关掉可跳过整个校验。
 * - `kudos.ms.sys.startup.dict-types-validation.fail-on-missing` — 默认 `false`。
 *   `true` 时若发现缺失则抛 [IllegalStateException]，**应用启动失败**；适合 CI / 预发环境。
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

    @Volatile
    var lastResult: ValidationResult? = null
        private set

    @EventListener(ApplicationReadyEvent::class)
    open fun onApplicationReady() {
        if (!enabled) {
            log.info("SysDictTypes 启动校验已禁用（kudos.ms.sys.startup.dict-types-validation.enabled=false）")
            return
        }
        val result = validate(SysConsts.ATOMIC_SERVICE_NAME)
        lastResult = result
        if (result.missing.isNotEmpty()) {
            val msg = "SysDictTypes 启动校验失败：以下字典类型在 sys_dict 表中找不到 active 记录（atomicServiceCode=${SysConsts.ATOMIC_SERVICE_NAME}）：" +
                    "${result.missing}。受影响的 @DictItemCode 校验将永远失败，请检查 Flyway 种子数据或代码常量。"
            if (failOnMissing) {
                throw IllegalStateException(msg)
            } else {
                log.error(msg)
            }
        }
        if (result.extras.isNotEmpty()) {
            log.warn(
                "sys_dict 中存在 atomicServiceCode={0} 但未在 SysDictTypes 中声明的 dict_type：{1}。" +
                        "可能是历史遗留或新加未同步常量。",
                SysConsts.ATOMIC_SERVICE_NAME, result.extras,
            )
        }
        if (result.missing.isEmpty() && result.extras.isEmpty()) {
            log.info("SysDictTypes 启动校验通过：共 {0} 项 dict_type 与数据库一致。", result.declared.size)
        }
    }

    /**
     * 执行一次校验并返回结果（不副作用于日志）。
     *
     * @param atomicServiceCode 比对范围；生产中固定为 [SysConsts.ATOMIC_SERVICE_NAME]，
     *   测试可传独立编码避免与共享 `sys_dict` 种子数据冲突。
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
     * 反射读取 [SysDictTypes] 中所有 String 类型 const val。
     * Kotlin `object` 的 `const val` 在 JVM 上是 public static final 字段，
     * 用 Java 反射直接读最稳，避免 `KClass.memberProperties` 在 const 场景下的边界行为。
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
        /** 代码常量声明的所有字典类型 */
        val declared: Set<String>,
        /** 声明了但数据库中不存在（active=true）的字典类型 —— **必须修复** */
        val missing: List<String>,
        /** 数据库中存在但代码常量未声明 —— 警告，可能是历史遗留 */
        val extras: List<String>,
    ) {
        val isOk: Boolean get() = missing.isEmpty()
    }
}
