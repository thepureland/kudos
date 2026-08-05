package io.kudos.tools.codegen.core

import io.kudos.tools.codegen.CodegenTestSupport
import io.kudos.tools.codegen.model.vo.ColumnInfo
import kotlin.test.*

/**
 * test for TemplatePathProcessor
 *
 * Covers:
 * - readPaths(false): macro.include filtering, exclusion of entity-related templates both by path
 *   (${entityName} in the file name) and by content, placeholder resolution in directory names,
 *   dot-to-slash package-style directory mapping, and result sorting by directory.
 * - readPaths(true): inclusion of entity templates with the full (H2-backed) entity model and
 *   ${entityName} resolution in the file name.
 * - the ".jar" template-root branch (classpath scan yielding no files for an absent jar path).
 *
 * @author K
 * @since 1.0.0
 */
internal class TemplatePathProcessorTest {

    private fun bindConfig(rootDir: String, codeLocation: String = "C:/codegen-out") {
        CodeGeneratorContext.config = CodegenTestSupport.newConfig(
            templateName = "tpp",
            templateRootDir = rootDir,
            moduleName = "tpp",
            codeLocation = codeLocation,
        )
        CodeGeneratorContext.templateModelCreator = TemplateModelCreator()
    }

    @Test
    fun readPathsWithoutEntityTemplates() {
        bindConfig(CodegenTestSupport.classpathDirPath("/templates/tpp"))

        val genFiles = TemplatePathProcessor.readPaths(false)

        val relativePaths = genFiles.map { it.finalFileRelativePath }
        assertEquals(
            setOf("a/b/deep.txt", "tpppkg/mod.txt", "dir1/plain.txt"),
            relativePaths.toSet(),
            "entity templates (by name or content) and macro.include must be excluded"
        )
        // sorted by directory
        assertEquals(relativePaths.sortedBy { it.substringBeforeLast('/') }, relativePaths)

        val plain = genFiles.first { it.getFilename() == "plain.txt" }
        assertFalse(plain.getGenerate(), "generate flag defaults to false")
        assertEquals("C:/codegen-out/dir1", plain.getDirectory())
        assertEquals("dir1/plain.txt", plain.templateFileRelativePath)
    }

    @Test
    fun readPathsWithEntityTemplatesResolvesEntityName() {
        CodegenTestSupport.bindNewH2(
            "tools_tpp",
            """CREATE TABLE IF NOT EXISTS "tpp_demo" (
                 "id" CHAR(36) DEFAULT RANDOM_UUID() NOT NULL PRIMARY KEY,
                 "name" VARCHAR(64) NOT NULL
               )""",
        )
        bindConfig(CodegenTestSupport.classpathDirPath("/templates/tpp"))
        CodeGeneratorContext.tableName = "tpp_demo"
        CodeGeneratorContext.tableComment = "demo"
        CodeGeneratorContext.columns = listOf(ColumnInfo().apply { setName("name") })

        val genFiles = TemplatePathProcessor.readPaths(true)

        val relativePaths = genFiles.map { it.finalFileRelativePath }.toSet()
        assertEquals(
            setOf(
                "a/b/deep.txt", "tpppkg/mod.txt", "dir1/plain.txt",
                "dir1/TppDemoDao.kt", "dir2/byContent.txt"
            ),
            relativePaths,
            "entity templates must be included with \${entityName} resolved"
        )
    }

    @Test
    fun readPathsWithJarRootScansClasspathAndToleratesAbsentJar() {
        bindConfig("templates/no_such_template_pack.jar")
        assertTrue(TemplatePathProcessor.readPaths(false).isEmpty())
    }
}
