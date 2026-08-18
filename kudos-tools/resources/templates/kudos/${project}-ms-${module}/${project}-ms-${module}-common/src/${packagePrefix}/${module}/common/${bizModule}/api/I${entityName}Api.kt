package ${packagePrefix}.${module}.common.${bizModule}.api

import ${packagePrefix}.${module}.common.${bizModule}.vo.${entityName}CacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


<@generateClassComment "External API for "+(table.comment!table.name)+"."/>
//region your codes 1
interface I${entityName}Api {
//endregion your codes 1

    /**
     * Get the record by primary key.
     *
     * @param id primary key
     * @return cache entry, or null if not found
     * @author ${author}
     * @since ${version}
     */
    @GetMapping("/api/internal/${module}/${shortEntityName?uncap_first}/get")
    fun get(@RequestParam id: ${pkColumn.kotlinTypeName}): ${entityName}CacheEntry?

    //region your codes 2

    //endregion your codes 2

}
