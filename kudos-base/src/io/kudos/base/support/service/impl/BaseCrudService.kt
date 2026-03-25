package io.kudos.base.support.service.impl

import io.kudos.base.bean.BeanKit
import io.kudos.base.enums.impl.CommonErrorCodeEnum
import io.kudos.base.error.ServiceException
import io.kudos.base.lang.GenericKit
import io.kudos.base.model.contract.common.IHasBuiltIn
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.model.payload.ISearchPayload
import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.base.model.payload.MutableListSearchPayload
import io.kudos.base.model.payload.UpdatePayload
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.support.dao.IBaseCrudDao
import io.kudos.base.support.service.iservice.IBaseCrudService
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * 基础可写业务操作：将 [IBaseCrudService] 委托给 [IBaseCrudDao]，仅依赖 base 中的 DAO 契约，不绑定 Ktorm。
 * 事务由 Spring 等容器在具体业务 Service 实现类上使用 `@Transactional` 等声明；本模块不依赖 Spring。
 *
 * 删除说明：当实体类型 [E] 实现 [IHasBuiltIn] 时，所有删除入口仅允许删除 `builtIn == false` 的行；
 * 若调用方针对内置行（`builtIn == true`）删除，将抛出 [ServiceException]（[CommonErrorCodeEnum.BUILTIN_NOT_DELETABLE]）。
 * 实现上在 Service 层拼接 `builtIn` 条件并委托现有 DAO，不修改 DAO；校验阶段尽量只查 `builtIn`（或 `id`+`builtIn`）列，避免 `get` 整行。
 *
 * @param PK 实体主键类型
 * @param E 实体类型，须实现 [IIdEntity]
 * @param DAO 可写 DAO 实现类型（须同时满足只读能力，见 [IBaseCrudDao] 继承 [io.kudos.base.support.dao.IBaseReadOnlyDao]）
 * @author K
 * @since 1.0.0
 */
open class BaseCrudService<PK : Any, E : IIdEntity<PK>, DAO : IBaseCrudDao<PK, E>>(dao: DAO) :
    BaseReadOnlyService<PK, E, DAO>(dao), IBaseCrudService<PK, E> {

    override fun insert(any: Any): PK = dao.insert(any)

    override fun insertOnly(entity: E, vararg propertyNames: String): PK = dao.insertOnly(entity, *propertyNames)

    override fun insertExclude(entity: E, vararg excludePropertyNames: String): PK =
        dao.insertExclude(entity, *excludePropertyNames)

    override fun batchInsert(objects: Collection<Any>, countOfEachBatch: Int): Int =
        dao.batchInsert(objects, countOfEachBatch)

    override fun batchInsertOnly(entities: Collection<E>, countOfEachBatch: Int, vararg propertyNames: String): Int =
        dao.batchInsertOnly(entities, countOfEachBatch, *propertyNames)

    override fun batchInsertExclude(
        entities: Collection<E>, countOfEachBatch: Int, vararg excludePropertyNames: String
    ): Int = dao.batchInsertExclude(entities, countOfEachBatch, *excludePropertyNames)

    override fun update(any: Any): Boolean = dao.update(any)

    override fun updateWhen(entity: E, criteria: Criteria): Boolean = dao.updateWhen(entity, criteria)

    override fun updateProperties(id: PK, properties: Map<String, *>): Boolean =
        dao.updateProperties(id, properties)

    override fun updatePropertiesWhen(id: PK, properties: Map<String, *>, criteria: Criteria): Boolean =
        dao.updatePropertiesWhen(id, properties, criteria)

    override fun updateOnly(entity: E, vararg propertyNames: String): Boolean = dao.updateOnly(entity, *propertyNames)

    override fun updateOnlyWhen(entity: E, criteria: Criteria, vararg propertyNames: String): Boolean =
        dao.updateOnlyWhen(entity, criteria, *propertyNames)

    override fun updateExcludePropertiesWhen(
        entity: E, criteria: Criteria, vararg excludePropertyNames: String
    ): Boolean = dao.updateExcludePropertiesWhen(entity, criteria, *excludePropertyNames)

    override fun batchUpdate(entities: Collection<E>, countOfEachBatch: Int): Int =
        dao.batchUpdate(entities, countOfEachBatch)

    override fun batchUpdateWhen(entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int): Int =
        dao.batchUpdateWhen(entities, criteria, countOfEachBatch)

    override fun <S : ISearchPayload> batchUpdateWhen(updatePayload: UpdatePayload<S>): Int =
        dao.batchUpdateWhen(updatePayload)

    override fun updateExcludeProperties(entity: E, vararg excludePropertyNames: String): Boolean =
        dao.updateExcludeProperties(entity, *excludePropertyNames)

    override fun batchUpdateProperties(criteria: Criteria, properties: Map<String, *>): Int =
        dao.batchUpdateProperties(criteria, properties)

    override fun batchUpdateOnly(entities: Collection<E>, countOfEachBatch: Int, vararg propertyNames: String): Int =
        dao.batchUpdateOnly(entities, countOfEachBatch, *propertyNames)

    override fun batchUpdateOnlyWhen(
        entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int, vararg propertyNames: String
    ): Int = dao.batchUpdateOnlyWhen(entities, criteria, countOfEachBatch, *propertyNames)

    override fun batchUpdateExcludeProperties(
        entities: Collection<E>, countOfEachBatch: Int, vararg excludePropertyNames: String
    ): Int = dao.batchUpdateExcludeProperties(entities, countOfEachBatch, *excludePropertyNames)

    override fun batchUpdateExcludePropertiesWhen(
        entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int, vararg excludePropertyNames: String
    ): Int = dao.batchUpdateExcludePropertiesWhen(entities, criteria, countOfEachBatch, *excludePropertyNames)

    //region Delete（含 IHasBuiltIn 内置行保护，仅 Service 层实现、不改 DAO）

    /**
     * 当前具体 Service 子类上 [BaseCrudService] 的第二个泛型参数，即实体类型 [E]。
     * 用于判断是否实现 [IHasBuiltIn] 以及解析 [builtIn] 列对应的属性反射。
     */
    @Suppress("UNCHECKED_CAST")
    private val entityClass: KClass<E> by lazy {
        GenericKit.getSuperClassGenricClass(this::class, 1) as KClass<E>
    }

    /**
     * 为 true 表示 [E] 带内置标记字段，删除方法走「仅 builtIn == false 可删」分支；否则与原生 [dao] 行为一致。
     */
    private val hasBuiltInField: Boolean by lazy {
        IHasBuiltIn::class.java.isAssignableFrom(entityClass.java)
    }

    /**
     * 实体上与 [IHasBuiltIn.builtIn] 同名的属性，用于 [searchProperty] / [searchProperties] 只拉取布尔列做失败原因判断。
     */
    @Suppress("UNCHECKED_CAST")
    private val builtInProperty: KProperty1<E, Boolean?> by lazy {
        entityClass.memberProperties.first { it.name == IHasBuiltIn::builtIn.name } as KProperty1<E, Boolean?>
    }

    private val idPropertyName: String
        get() = IIdEntity<PK>::id.name

    private val builtInPropertyName: String
        get() = IHasBuiltIn::builtIn.name

    /** 删除/排除内置行：等价于 SQL 条件 `built_in = false`（属性名以实体为准）。 */
    private fun builtInFalseCriteria(): Criteria =
        Criteria.of(builtInPropertyName, OperatorEnum.EQ, false)

    /** 用于判断「给定业务条件下是否存在内置行」（仅探测，不删数据）。 */
    private fun builtInTrueCriteria(): Criteria =
        Criteria.of(builtInPropertyName, OperatorEnum.EQ, true)

    private fun throwBuiltInNotDeletable(): Nothing =
        throw ServiceException(CommonErrorCodeEnum.BUILTIN_NOT_DELETABLE)

    /**
     * 复制 [ListSearchPayload] 的 WHERE/分页等语义，通过 [MutableListSearchPayload.setReturnProperties] 限定只 SELECT 主键与 [builtIn]。
     * 供 [batchDeleteWhenForBuiltInEntity] 使用，避免加载完整 PO，并与原 payload 行为对齐。
     */
    private fun toIdBuiltInProbePayload(source: ListSearchPayload): MutableListSearchPayload {
        val probe = MutableListSearchPayload()
        BeanKit.copyProperties(source, probe)
        probe.setReturnProperties(listOf(idPropertyName, builtInPropertyName))
        return probe
    }

    /**
     * 按主键删除（内置表）：`WHERE id = ? AND builtIn = false`。
     * - 影响 1 行：成功。
     * - 影响 0 行：仅查询该主键的 [builtIn]；若为 true 则抛内置不可删；否则视为记录不存在，返回 false。
     */
    private fun deleteByIdForBuiltInEntity(id: PK): Boolean {
        val deleteCriteria = Criteria(idPropertyName, OperatorEnum.EQ, id).addAnd(builtInFalseCriteria())
        if (dao.batchDeleteCriteria(deleteCriteria) == 1) {
            return true
        }
        val builtInValues = searchProperty(Criteria(idPropertyName, OperatorEnum.EQ, id), builtInProperty)
        if (builtInValues.isNotEmpty() && builtInValues.first() == true) {
            throwBuiltInNotDeletable()
        }
        return false
    }

    /**
     * 按主键集合删除（内置表）：`WHERE id IN (...) AND builtIn = false`（入参先 [distinct]）。
     * 若删除行数小于去重后主键个数，则一次 [inSearchPropertiesById] 拉取 `id`+`builtIn`：
     * 仍存在 `builtIn == true` 的主键则抛异常；否则解释为部分主键本就不存在，返回实际删除行数。
     *
     * 注意：与内置行混删时，非内置行可能已在同一 DELETE 中删掉，再抛错；调用方需在事务语义下接受该行为。
     */
    private fun batchDeleteForBuiltInEntity(ids: Collection<PK>): Int {
        require(ids.isNotEmpty()) { "批量删除实体对象时，主键集合不能为空！" }
        val distinctIds = ids.distinct()
        val deleteCriteria =
            Criteria(idPropertyName, OperatorEnum.IN, distinctIds.toList()).addAnd(builtInFalseCriteria())
        val deleted = dao.batchDeleteCriteria(deleteCriteria)
        if (deleted == distinctIds.size) {
            return deleted
        }
        val rows = inSearchPropertiesById(distinctIds, listOf(idPropertyName, builtInPropertyName))
        if (rows.any { (it[builtInPropertyName] as? Boolean) == true }) {
            throwBuiltInNotDeletable()
        }
        return deleted
    }

    /**
     * 按 [Criteria] 删除（内置表）：在原条件上 AND `builtIn = false`。
     * 若删除 0 行，再用「原条件 AND builtIn = true」仅 SELECT [builtIn] 列做探测：
     * 有命中则说明范围内存在内置行、不允许按当前语义删除，抛异常；无命中则返回 0（无匹配）。
     */
    private fun batchDeleteCriteriaForBuiltInEntity(criteria: Criteria): Int {
        require(!criteria.isEmpty()) { "批量删除实体对象时，查询条件不能为空！" }
        val safeCriteria = Criteria().addAnd(criteria).addAnd(builtInFalseCriteria())
        val deleted = dao.batchDeleteCriteria(safeCriteria)
        if (deleted > 0) {
            return deleted
        }
        val probeCriteria = Criteria().addAnd(criteria).addAnd(builtInTrueCriteria())
        val builtInProbe = searchProperties(probeCriteria, listOf(builtInProperty))
        if (builtInProbe.isNotEmpty()) {
            throwBuiltInNotDeletable()
        }
        return deleted
    }

    /**
     * 按查询载体删除（内置表）。不改 DAO 时无法向 `batchDeleteWhen` 追加表达式，故要求 [searchPayload] 为 [ListSearchPayload]。
     *
     * 流程：将载体复制为仅返回 `id`+[builtIn] 的探测查询 → 若结果中含内置行则抛异常 → 否则对筛出的主键调用 [batchDeleteForBuiltInEntity]。
     */
    private fun batchDeleteWhenForBuiltInEntity(searchPayload: ISearchPayload): Int {
        require(searchPayload is ListSearchPayload) {
            "实现IHasBuiltIn的实体执行batchDeleteWhen时，searchPayload必须是ListSearchPayload。"
        }
        val probePayload = toIdBuiltInProbePayload(searchPayload)
        @Suppress("UNCHECKED_CAST")
        val rows = dao.search(probePayload) as List<Map<String, Any?>>
        if (rows.any { (it[builtInPropertyName] as? Boolean) == true }) {
            throwBuiltInNotDeletable()
        }
        @Suppress("UNCHECKED_CAST")
        val ids = rows.mapNotNull { @Suppress("UNCHECKED_CAST") it[idPropertyName] as? PK }
        if (ids.isEmpty()) {
            return 0
        }
        return batchDeleteForBuiltInEntity(ids)
    }

    /** 按主键删除；若 [E] 实现 [IHasBuiltIn]，仅删除非内置行。 */
    override fun deleteById(id: PK): Boolean =
        if (!hasBuiltInField) dao.deleteById(id) else deleteByIdForBuiltInEntity(id)

    /** 批量按主键删除；若 [E] 实现 [IHasBuiltIn]，仅删除非内置行。 */
    override fun batchDelete(ids: Collection<PK>): Int =
        if (!hasBuiltInField) dao.batchDelete(ids) else batchDeleteForBuiltInEntity(ids)

    /** 按条件批量删除；若 [E] 实现 [IHasBuiltIn]，自动附加 `builtIn = false`。 */
    override fun batchDeleteCriteria(criteria: Criteria): Int =
        if (!hasBuiltInField) dao.batchDeleteCriteria(criteria) else batchDeleteCriteriaForBuiltInEntity(criteria)

    /**
     * 按查询载体批量删除；若 [E] 实现 [IHasBuiltIn]，[searchPayload] 须为 [ListSearchPayload]（见 [batchDeleteWhenForBuiltInEntity]）。
     */
    override fun batchDeleteWhen(searchPayload: ISearchPayload): Int =
        if (!hasBuiltInField) dao.batchDeleteWhen(searchPayload) else batchDeleteWhenForBuiltInEntity(searchPayload)

    /** 删除实体对应行；内置表等价于 [deleteById]（以数据库中 builtIn 为准）。 */
    override fun delete(entity: E): Boolean =
        if (!hasBuiltInField) dao.delete(entity) else deleteByIdForBuiltInEntity(entity.id)

    //endregion Delete

}
