package io.kudos.tools.sql

import io.kudos.base.io.FileKit
import java.io.File
import java.sql.DriverManager

/**
 * 用于批量执行文件中的sql语句。
 * SQL 来源于本地文件（非用户输入），为可信内容，故对 addBatch(line) 的“不安全 SQL 字符串”告警予以抑制。
 *
 * @author K
 * @since 1.0.0
 */
fun main() {
    val file = "C:\\Users\\hanfei\\Desktop\\area2019.sql" // 文件编码一定要是UTF8无签名，不然会出现莫名其妙的错误
    val lineIterator = FileKit.lineIterator(File(file), "UTF-8")


//    Class.forName("org.h2.Driver")
    val conne = DriverManager.getConnection(
        "jdbc:h2:tcp://localhost:9092/D:/dev/kudos/kudos-data/kudos-data-jdbc/h2/h2",
        "sa",
        null
    )
    val stm = conne.createStatement()
    val start = System.currentTimeMillis()
    conne.autoCommit = false
    var i = 0

    while (lineIterator.hasNext()) {
        val line = lineIterator.next()
        @Suppress("SqlSourceToSinkFlow")
        stm.addBatch(line)
        i++
        if (i % 5000 == 0) {
            stm.executeBatch()
            conne.commit()
            println("共提交${i}条")
        }
    }

    if(!lineIterator.hasNext()) {
        stm.executeBatch()
        conne.commit()
    }

    val end = System.currentTimeMillis()
    println("添加${i}条数据总共耗时：${end - start}ms")
    stm.close()
    conne.close()

}