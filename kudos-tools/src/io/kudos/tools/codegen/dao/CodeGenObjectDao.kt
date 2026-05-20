package io.kudos.tools.codegen.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.tools.codegen.model.po.CodeGenObject
import io.kudos.tools.codegen.model.table.CodeGenObjects
import org.ktorm.dsl.eq
import org.ktorm.entity.firstOrNull
import org.springframework.stereotype.Repository

/**
 * 代码生成-对象信息数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
object CodeGenObjectDao: BaseCrudDao<String, CodeGenObject, CodeGenObjects>() {
//endregion your codes 1

    //region your codes 2

    fun searchByName(name: String): CodeGenObject? =
        entitySequence().firstOrNull { it.name eq name }

    //endregion your codes 2

}