package io.kudos.base.support.iservice

import io.kudos.base.query.Criteria
import io.kudos.base.support.IIdEntity
import io.kudos.base.support.payload.SearchPayload
import io.kudos.base.support.payload.UpdatePayload

/**
 * 基础业务操作接口
 * 
 * 定义了业务层的完整操作，包括查询、插入、更新、删除等功能。
 * 继承自IBaseReadOnlyService，在只读操作基础上增加了数据修改能力。
 * 基于关系型数据库表，提供统一的业务接口，简化业务层代码。
 * 
 * 核心功能：
 * 1. 查询操作：继承自IBaseReadOnlyService的所有查询方法
 * 2. 插入操作：支持单个和批量插入，支持指定属性插入和排除属性插入
 * 3. 更新操作：支持单个和批量更新，支持条件更新、指定属性更新
 * 4. 删除操作：支持根据主键删除、条件删除、批量删除
 * 
 * 插入操作：
 * - insert：插入实体或插入载体对象，返回主键值
 * - insertOnly：只插入指定的属性
 * - insertExclude：插入除指定属性外的所有属性
 * - batchInsert：批量插入，支持分批处理
 * 
 * 更新操作：
 * - update：更新实体或更新载体对象，只更新有变更的属性
 * - updateWhen：条件更新，仅当满足附加查询条件时更新
 * - updateProperties：更新指定的属性（Map形式）
 * - updateOnly：只更新指定的属性
 * - updateExcludeProperties：更新除指定属性外的所有属性
 * - batchUpdate：批量更新，支持分批处理
 * 
 * 删除操作：
 * - deleteById：根据主键删除
 * - delete：删除实体对象
 * - batchDelete：批量删除，支持根据主键列表或条件删除
 * 
 * 属性控制：
 * - 支持只操作指定属性（Only系列方法）
 * - 支持排除指定属性（Exclude系列方法）
 * - 主键属性永远不会被更新
 * 
 * 条件更新：
 * - updateWhen系列方法支持附加查询条件
 * - 仅当满足条件时才执行更新操作
 * - 返回是否更新成功或更新的记录数
 * 
 * 批量处理：
 * - 所有批量操作都支持countOfEachBatch参数
 * - 默认每批1000条记录，避免内存溢出
 * - 基于JDBC的executeBatch实现，性能优化
 * 
 * 与DAO层的关系：
 * - 业务层接口，通常委托给DAO层实现
 * - 可以在业务层添加业务逻辑处理（如数据校验、事务控制等）
 * - 提供统一的业务接口，隐藏DAO层细节
 * 
 * 使用场景：
 * - 业务层的数据操作
 * - RESTful API的服务层
 * - 批量数据导入和更新
 * 
 * 注意事项：
 * - 插入操作返回主键值，更新和删除操作返回是否成功或记录数
 * - 批量操作支持分批处理，避免一次性处理大量数据
 * - 条件更新需要提供查询条件，否则会抛出异常
 * - 主键属性在更新操作中会被自动排除
 * 
 * @param PK 实体主键类型
 * @param E 实体类型，必须实现IIdEntity接口
 * @since 1.0.0
 */
interface IBaseCrudService<PK : Any, E : IIdEntity<PK>> : IBaseReadOnlyService<PK, E> {

    //region Insert

    /**
     * 插入指定实体或“插入项载休”到当前表
     *
     * @param any 实体对象或插入项载休
     * @return 主键值
     * @author K
     * @since 1.0.0
     */
    fun insert(any: Any): PK

    /**
     * 保存实体对象，只保存指定的属性
     *
     * @param entity 实体对象
     * @param propertyNames 要保存的属性的可变数组
     * @return 主键值
     * @author K
     * @since 1.0.0
     */
    fun insertOnly(entity: E, vararg propertyNames: String): PK

    /**
     * 保存实体对象，不保存指定的属性
     *
     * @param entity 实体对象
     * @param excludePropertyNames 不保存的属性的可变数组
     * @return 主键值
     * @author K
     * @since 1.0.0
     */
    fun insertExclude(entity: E, vararg excludePropertyNames: String): PK

    /**
     * 批量插入指定实体或“插入项载休”到当前表。
     *
     * ktorm底层该方法是基于原生 JDBC 提供的 executeBatch 函数实现
     *
     * @param objects 实体对象集合或“插入项载休”集合
     * @param countOfEachBatch 每批大小，缺省为1000
     * @return 成功插入的记录数
     * @author K
     * @since 1.0.0
     */
    fun batchInsert(objects: Collection<Any>, countOfEachBatch: Int = 1000): Int

    /**
     * 批量保存实体对象，只保存指定的属性
     *
     * @param entities 实体对象列表
     * @param countOfEachBatch 每批大小，缺省为1000
     * @param propertyNames 要保存的属性的可变数组
     * @return 保存的记录数
     * @author K
     * @since 1.0.0
     */
    fun batchInsertOnly(entities: Collection<E>, countOfEachBatch: Int = 1000, vararg propertyNames: String): Int

    /**
     * 批量保存实体对象，不保存指定的属性
     *
     * @param entities 实体对象列表
     * @param countOfEachBatch 每批大小，缺省为1000
     * @param excludePropertyNames 不保存的属性的可变数组
     * @return 保存的记录数
     * @author K
     * @since 1.0.0
     */
    fun batchInsertExclude(
        entities: Collection<E>, countOfEachBatch: Int = 1000, vararg excludePropertyNames: String
    ): Int

    //endregion Insert


    //region Update

    /**
     * 更新指定实体或更新载体对应的记录
     *
     * @param any 实体对象或更新载体（如果是实体对象，只会对有更改的属性作更新；如果是更新载体，将对载体的所有属性作更新）
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun update(any: Any): Boolean

    /**
     * 有条件的更新实体对象（仅当满足给定的附加查询条件时）
     *
     * @param entity 实体对象
     * @param criteria 附加查询条件
     * @return 记录是否有更新
     * @throws IllegalArgumentException 条件为空时
     * @author K
     * @since 1.0.0
     */
    fun updateWhen(entity: E, criteria: Criteria): Boolean

    /**
     * 只更新实体的某几个属性
     *
     * @param id         主键值
     * @param properties Map(属性名，属性值)
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateProperties(id: PK, properties: Map<String, *>): Boolean

    /**
     * 有条件的只更新实体的某几个属性（仅当满足给定的附加查询条件时）
     * 注：id属性永远不会被更新
     *
     * @param id         主键值
     * @param properties Map(属性名，属性值)
     * @param criteria 附加查询条件
     * @return 记录是否有更新
     * @throws IllegalArgumentException 无查询条件时
     * @author K
     * @since 1.0.0
     */
    fun updatePropertiesWhen(id: PK, properties: Map<String, *>, criteria: Criteria): Boolean

    /**
     * 只更新实体的某几个属性
     *
     * @param entity     实体对象
     * @param propertyNames 更新的属性的可变数组
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateOnly(entity: E, vararg propertyNames: String): Boolean

    /**
     * 有条件的只更新实体的某几个属性（仅当满足给定的附加查询条件时）
     * 注：id属性永远不会被更新
     *
     * @param entity     实体对象
     * @param criteria 附加查询条件
     * @param propertyNames 更新的属性的可变数组
     * @return 记录是否有更新
     * @throws IllegalArgumentException 无查询条件时
     * @author K
     * @since 1.0.0
     */
    fun updateOnlyWhen(entity: E, criteria: Criteria, vararg propertyNames: String): Boolean

    /**
     * 有条件的更新实体除指定的几个属性外的所有属性（仅当满足给定的附加查询条件时）
     * 注：id属性永远不会被更新
     *
     * @param entity            实体对象
     * @param criteria 附加查询条件
     * @param excludePropertyNames 不更新的属性的可变数组
     * @return 记录是否有更新
     * @throws IllegalArgumentException 无查询条件时
     * @author K
     * @since 1.0.0
     */
    fun updateExcludePropertiesWhen(entity: E, criteria: Criteria, vararg excludePropertyNames: String): Boolean

    /**
     * 批量更新实体对应的记录
     *
     * ktorm底层该方法是基于原生 JDBC 提供的 executeBatch 函数实现
     *
     * @param entities 实体对象集合
     * @param countOfEachBatch 每批大小，缺省为1000
     * @return 更新成功的记录数
     * @throws IllegalStateException 存在主键为null时
     * @author K
     * @since 1.0.0
     */
    fun batchUpdate(entities: Collection<E>, countOfEachBatch: Int = 1000): Int

    /**
     * 有条件的批量更新指定属性
     * 更新规则见 @see UpdatePayload 类，查询规则见 @see SearchPayload
     *
     * @param S 查询项载体类型
     * @param updatePayload 更新项载体
     * @return 更新的记录数
     * @throws IllegalArgumentException 无查询条件时
     * @author K
     * @since 1.0.0
     */
    fun <S : SearchPayload> batchUpdateWhen(updatePayload: UpdatePayload<S>): Int

    /**
     * 有条件的批量更新实体对象（仅当满足给定的附加查询条件时）
     *
     * @param entities 实体对象集合
     * @param criteria 附加查询条件
     * @param countOfEachBatch 每批大小，缺省为1000
     * @return 更新的记录数
     * @throws IllegalArgumentException 无查询条件时
     * @author K
     * @since 1.0.0
     */
    fun batchUpdateWhen(entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int = 1000): Int

    /**
     * 更新实体除指定的几个属性外的所有属性
     * 注：id属性永远不会被更新
     *
     * @param entity            实体对象
     * @param excludePropertyNames 不更新的属性的可变数组
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateExcludeProperties(entity: E, vararg excludePropertyNames: String): Boolean

    /**
     * 批量更新实体对象, 只更新实体的某几个属性
     *
     * @param criteria   查询条件
     * @param properties Map(属性名，属性值)
     * @return 是否更新成功
     * @throws IllegalArgumentException 未指定要更新的属性或无查询条件时
     * @author K
     * @since 1.0.0
     */
    fun batchUpdateProperties(criteria: Criteria, properties: Map<String, *>): Int

    /**
     * 批量更新实体对象指定的几个属性
     *
     * @param entities   实体对象列表
     * @param countOfEachBatch 每批大小，缺省为1000
     * @param propertyNames 更新的属性的可变数组
     * @return 更新的记录数
     * @author K
     * @since 1.0.0
     */
    fun batchUpdateOnly(entities: Collection<E>, countOfEachBatch: Int = 1000, vararg propertyNames: String): Int

    /**
     * 有条件的批量更新实体对象指定的几个属性（仅当满足给定的附加查询条件时）
     * 注：id属性永远不会被更新
     *
     * @param entities   实体对象列表
     * @param criteria 附加查询条件
     * @param countOfEachBatch 每批大小，缺省为1000
     * @param propertyNames 更新的属性的可变数组
     * @return 更新的记录数
     * @throws IllegalArgumentException 无查询条件时
     * @author K
     * @since 1.0.0
     */
    fun batchUpdateOnlyWhen(
        entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int = 1000, vararg propertyNames: String
    ): Int

    /**
     * 批量更新实体除了指定几个属性外的所有属性
     * 注：id属性永远不会被更新
     *
     * @param entities   实体对象列表
     * @param countOfEachBatch 每批大小，缺省为1000
     * @param excludePropertyNames 不更新的属性的可变数组
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun batchUpdateExcludeProperties(
        entities: Collection<E>, countOfEachBatch: Int = 1000, vararg excludePropertyNames: String
    ): Int

    /**
     * 有条件的批量更新实体除了指定几个属性外的所有属性（仅当满足给定的附加查询条件时）
     * 注：id属性永远不会被更新
     *
     * @param entities   实体对象列表
     * @param criteria 附加查询条件
     * @param countOfEachBatch 每批大小，缺省为1000
     * @param excludePropertyNames 不更新的属性的可变数组
     * @return 是否更新成功
     * @throws IllegalArgumentException 无查询条件时
     * @author K
     * @since 1.0.0
     */
    fun batchUpdateExcludePropertiesWhen(
        entities: Collection<E>, criteria: Criteria, countOfEachBatch: Int = 1000, vararg excludePropertyNames: String
    ): Int

    //endregion Update


    //region Delete

    /**
     * 删除指定主键值对应的记录
     *
     * @param id 主键值，类型必须为以下之一：String、Int、Long
     * @return 是否删除成功
     * @author K
     * @since 1.0.0
     */
    fun deleteById(id: PK): Boolean

    /**
     * 批量删除指定主键对应的实体对象
     *
     * @param ids 主键列表
     * @return 删除的记录数
     * @author K
     * @since 1.0.0
     */
    fun batchDelete(ids: Collection<PK>): Int

    /**
     * 批量删除指定主键对应的实体对象
     *
     * @param criteria 查询条件
     * @return 删除的记录数
     * @throws IllegalArgumentException 无查询条件时
     * @author K
     * @since 1.0.0
     */
    fun batchDeleteCriteria(criteria: Criteria): Int

    /**
     * 批量删除指定条件的实体对象
     *
     * @param searchPayload 查询项载体
     * @return 删除的记录数
     * @throws IllegalArgumentException 无查询条件时
     * @author K
     * @since 1.0.0
     */
    fun batchDeleteWhen(searchPayload: SearchPayload): Int

    /**
     * 删除实体对应的记录
     *
     * @param entity 实体
     * @return 是否删除成功
     * @author K
     * @since 1.0.0
     */
    fun delete(entity: E): Boolean

    //endregion Delete

}