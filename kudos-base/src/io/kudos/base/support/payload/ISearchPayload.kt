package io.kudos.base.support.payload

import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.logic.AndOrEnum
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0


/**
 * 不可变查询条件载体（接口）
 *
 * 用于封装查询条件，支持灵活的查询规则配置和结果类型控制。
 * 实现类通过**在类中明确定义属性**来自动生成查询条件，简化查询代码的编写。
 *
 * **安全性设计**：本接口仅提供 [getAndOr]/[getOperators] 等方法，实现类只能 override 这些方法而无法暴露 setter，
 * 从而无法在实例创建后由外部修改查询结构。适合承载来自不信任源（如前端、开放 API）的查询参数，降低注入与越权风险。
 *
 * 核心功能：
 * 1. 自动条件生成：根据属性值自动生成查询条件
 * 2. 灵活操作符：支持为每个属性指定不同的操作符（子类通过重写 [getOperators] 等方法返回固定值）
 * 3. 逻辑关系控制：支持 AND 和 OR 两种逻辑关系
 * 4. 结果类型控制：支持返回实体、属性值或 Map 等多种结果类型
 * 5. 自定义条件：支持完全自定义的查询条件
 *
 * 查询规则：
 * 1. 空值过滤：各个属性只有当其值非空时才会应用该查询条件
 *    - 例外：[getNullProperties] 返回列表中指定的属性，即使值为 null 也会作为查询条件
 * 2. 默认操作符：各个属性的查询逻辑默认都是等于（EQ）
 *    - 可通过 [getOperators] 为特定属性指定不同的操作符
 * 3. 逻辑关系：各属性间的关系默认为 AND
 *    - 可通过重写 [getAndOr] 改变为 OR
 *
 * 结果类型控制：
 * - [getReturnProperties] 为空：返回 [getReturnEntityClass] 指定的实体列表，或表对应的 PO 列表
 * - [getReturnProperties] 为单个属性：返回该属性值的列表（如 List<String>）
 * - [getReturnProperties] 为多个属性：返回 Map(属性名, 属性值) 的列表
 *
 * 优先级：
 * - [getCriterions]（最高）：完全自定义的查询条件，会覆盖所有自动生成的条件
 * - [getOperators]：为特定属性指定操作符，覆盖默认的等于操作
 * - 默认规则：属性值非空时使用等于操作，属性间 AND 关系
 *
 * 使用场景：
 * - RESTful API 的查询参数封装（推荐：由子类固定结构，请求只填充属性值）
 * - 需要防止外部篡改查询条件的场景
 *
 * 注意事项：
 * - 实现类需要定义查询属性，属性名对应数据库字段或实体属性
 * - [getReturnEntityClass] 返回类型的属性可以比 PO 多，但只会自动封装名字一致的属性
 * - [getNullProperties] 用于处理需要查询 null 值的特殊场景
 *
 * @author K
 * @since 1.0.0
 */
interface ISearchPayload {

    /** 各属性间的查询逻辑关系，默认为 AND。实现类通过重写此方法定制，不可改为可写属性。 */
    fun getAndOr(): AndOrEnum = AndOrEnum.AND

    /** 值为 null 时须作为查询条件的属性的列表。 */
    fun getNullProperties(): List<String>? = null

    /**
     * 属性的特殊(非等于)查询操作逻辑，key 必须为该类的属性引用(KProperty0)。
     */
    fun getOperators(): Map<KProperty0<*>, OperatorEnum>? = null

    /**
     * 返回的实体类型。
     * 仅当 [getReturnProperties] 为空时才会应用；
     * 若此时为 null，将返回所查询表对应的 PO。
     * 该类型中定义的属性可以比 PO 多，但只会自动封装名字一致的(类型要能兼容)。
     */
    fun getReturnEntityClass(): KClass<*>? = null

    /**
     * 查询结果的属性列表。
     * 若为空，则为 [getReturnEntityClass] 对象列表或表对应的 PO 列表；
     * 若单个属性，则为该属性值的列表；
     * 若多个属性，则为 Map(属性名, 属性值) 的列表。
     */
    fun getReturnProperties(): List<String>? = null

    /** 完全自定义的属性查询逻辑。优先级最高，会覆盖原来的查询逻辑。 */
    fun getCriterions(): List<Criterion>? = null

}
