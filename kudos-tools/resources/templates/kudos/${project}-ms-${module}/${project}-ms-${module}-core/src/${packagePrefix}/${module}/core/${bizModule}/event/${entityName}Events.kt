package ${packagePrefix}.${module}.core.${bizModule}.event


<@generateClassComment "Domain events of "+(table.comment!table.name)+" (`"+table.name+"`)."/>
//region your codes 1
sealed interface ${entityName}Event {
    val id: ${pkColumn.kotlinTypeName}
}
//endregion your codes 1

data class ${entityName}Inserted(override val id: ${pkColumn.kotlinTypeName}) : ${entityName}Event
data class ${entityName}Updated(override val id: ${pkColumn.kotlinTypeName}) : ${entityName}Event
data class ${entityName}Deleted(override val id: ${pkColumn.kotlinTypeName}) : ${entityName}Event

/** Batch delete: carries every deleted primary key. Cache invalidation dimensions must be added here. */
data class ${entityName}BatchDeleted(
    val ids: Collection<${pkColumn.kotlinTypeName}>,
) : ${entityName}Event {
    override val id: ${pkColumn.kotlinTypeName} get() = ids.first()
}

//region your codes 2

//endregion your codes 2
