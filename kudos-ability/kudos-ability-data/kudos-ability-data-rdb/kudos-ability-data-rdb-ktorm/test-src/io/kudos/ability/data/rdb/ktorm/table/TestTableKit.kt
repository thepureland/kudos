package io.kudos.ability.data.rdb.ktorm.table

import io.kudos.ability.data.rdb.jdbc.kit.RdbKit
import io.kudos.context.kit.SpringKit
import org.soul.ability.data.rdb.jdbc.metadata.RdbTypeEnum
import org.soul.base.scanner.classpath.ClassPathScanner
import org.soul.base.scanner.support.Resource
import org.springframework.jdbc.core.JdbcTemplate

/**
 * 测试表工具类
 *
 * @author K
 * @since 1.0.0
 */
internal object TestTableKit {

    const val TABLE_NAME = "test_table"

    private const val SCRIPT_CLASSPATH = "sql"
    private const val SCRIPT_SUFFIX = ".sql"

    fun create() {
        operateTestTable("create")
    }

    fun insert(): IntArray? {
        operateTestTable("insert")
        val ids = IntArray(11)
        for (i in 0..10) {
            ids[i] = i
        }
        return ids
    }

    fun drop() {
        operateTestTable("drop")
    }

    private fun operateTestTable(operate: String) {
        val productName = RdbKit.getDataSource().connection.metaData.databaseProductName
        val rdbType = RdbTypeEnum.ofProductName(productName)
        var path = SCRIPT_CLASSPATH
        if (rdbType == RdbTypeEnum.MYSQL || rdbType == RdbTypeEnum.SQLITE) {
            path += "/${productName.lowercase()}"
        }
        val resources: Array<Resource> = ClassPathScanner.scanForResources(path, operate, SCRIPT_SUFFIX)
        val sql = resources[0].loadAsString("UTF-8")!!
        SpringKit.getBean(JdbcTemplate::class).execute(sql)
    }

}