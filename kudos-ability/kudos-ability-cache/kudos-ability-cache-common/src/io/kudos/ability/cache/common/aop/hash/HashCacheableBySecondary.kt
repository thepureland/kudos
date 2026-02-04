package io.kudos.ability.cache.common.aop.hash

import io.kudos.base.support.IIdEntity
import kotlin.reflect.KClass

/**
 * 按副属性等值查询的缓存注解：先按「索引 + 查询值」查 Hash 缓存的 listBySetIndex，未命中则执行方法并将结果 saveBatch 回写。
 *
 * **[filterExpressions] 过滤表达式**数组的每一项 = 一个「按副属性等值」的查询维度（既决定用哪个索引，又决定该维度的查询值）：
 * - 每项须为**单参数 SpEL**（如 `#type`、`#subSystemCode`）。
 * - **索引名**：由该项推导，规则为 `#paramName` → 索引名 `paramName`（与实体副属性名一致，且需在 [filterableProperties] 中）。
 * - **查询值**：该项 SpEL 在方法调用时从参数求值的结果，作为 `listBySetIndex(cacheName, entityClass, property, value)` 的 `value`。
 * - **单元素**：单条件等值，如 `["#type"]` 即按 type 索引等值查。
 * - **多元素**：多条件等值 **AND**，如 `["#subSystemCode", "#url"]` 即按 subSystemCode、url 分别 listBySetIndex 后按 id 取交集。
 *
 * 每项须为 `#paramName` 形式；复合表达式（如 `"#a + '_' + #b"`）不支持，多条件请用多元素。
 *
 * 术语：**主属性**为 id；**副属性**由 [filterableProperties]/[sortableProperties] 表示，回写时建索引。
 * 方法返回值支持：List&lt;IIdEntity&gt;（命中返回列表并负责 saveBatch）、String?（命中返回首个 id，未命中回写由方法体完成）、List&lt;String&gt;（命中返回 id 列表，未命中回写由方法体完成）。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class HashCacheableBySecondary(

    /** Hash 缓存名称（需在配置中存在且 hash=true） */
    val cacheNames: Array<String> = [],

    /**
     * 过滤表达式：每项为单参数 SpEL（如 "#type"、"#subSystemCode"），
     * 切面用参数名作为 listBySetIndex 的 property（索引名）、用 SpEL 求值结果作为 value（查询值）。
     * 单元素即单条件，多元素即多条件 AND（多次 listBySetIndex 后按 id 取交集）。示例：["#type"]、["#subSystemCode", "#url"]。
     */
    val filterExpressions: Array<String> = ["#type"],

    /** 是否走缓存的 SpEL 条件，为空或解析为 true 时才查/写缓存。 */
    val condition: String = "",

    /** 是否不缓存的 SpEL 条件，解析为 true 时方法结果不写入缓存。 */
    val unless: String = "",

    /** 缓存实体类型，用于 listBySetIndex / saveBatch 的 KClass。 */
    val entityClass: KClass<out IIdEntity<*>>,

    /**
     * 可筛选副属性名（等值查询用 Set 索引），回写时建索引；需包含 [filterExpressions] 中各元素推导出的索引名。例外：数值型范围查询条件放 [sortableProperties]。
     */
    val filterableProperties: Array<String> = [],

    /** 可排序/范围副属性名（ZSet 索引），回写时可选；数值型范围查询条件放本项。 */
    val sortableProperties: Array<String> = []
)
