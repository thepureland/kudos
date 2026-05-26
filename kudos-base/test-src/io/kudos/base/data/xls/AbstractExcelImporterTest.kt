package io.kudos.base.data.xls

import io.kudos.base.io.PathKit
import jakarta.validation.constraints.NotNull
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * AbstractExcelImporter test cases
 *
 * @author K
 * @since 1.0.0
 */
internal class AbstractExcelImporterTest {

    @Test
    fun import() {

        val path = PathKit.getResourcePath("TestExcelImporter.xls")

        // Row object class as a data class
        assertEquals(3, TestStudentExcelImporter().import(File(path)).size)

        // Row object class as a regular class
        assertEquals(3, TestPersonExcelImporter().import(File(path)).size)
    }

    private class TestStudentExcelImporter : AbstractExcelImporter<TestStudent>() {

        override fun getPropertyNames(): List<String> = listOf("name", "sex", "age", "height")

        override fun getSheetName(): String = "students"

        override fun save(rowObjects: List<TestStudent>) {
            // Simulate saving data to DB
        }

    }

    internal data class TestStudent(

        @get:NotNull
        val height: Double,

        @get:NotNull
        val name: String,

        @get:NotNull
        val sex: String,

        @get:NotNull
        val age: Int

    )

    private class TestPersonExcelImporter : AbstractExcelImporter<TestPerson>() {

        override fun getPropertyNames(): List<String> = listOf("name", "sex", "age", "height")

        override fun getSheetName(): String = "students"

        override fun save(rowObjects: List<TestPerson>) {
            // Simulate saving data to DB
        }

    }

    internal class TestPerson {

        @get:NotNull
        var name: String? = null

        @get:NotNull
        var sex: String? = null

        @get:NotNull
        var age: Int? = null

        @get:NotNull
        var height: Double? = null

    }

}