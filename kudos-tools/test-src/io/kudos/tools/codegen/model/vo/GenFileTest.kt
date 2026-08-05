package io.kudos.tools.codegen.model.vo

import kotlin.test.*

/**
 * test for GenFile
 *
 * Covers the constructor-initialized JavaFX properties (generate/filename/directory), the two plain
 * constructor-parameter fields (finalFileRelativePath/templateFileRelativePath) including reassignment,
 * every getter/setter/property accessor, and the compareTo contract that sorts by directory so the UI list
 * groups files by directory (less/equal/greater, self-comparison, Windows-style paths).
 *
 * Plain JavaFX beans, so no FX toolkit is required.
 *
 * @author K
 * @since 1.0.0
 */
internal class GenFileTest {

    private fun newGenFile(
        generate: Boolean = false,
        filename: String = "Foo.kt",
        directory: String = "C:/out/dir",
        finalPath: String = "dir/Foo.kt",
        templatePath: String = "dir/Foo.kt.ftl",
    ) = GenFile(generate, filename, directory, finalPath, templatePath)

    @Test
    fun constructorInitializesAllFields() {
        val f = newGenFile(generate = true)
        assertTrue(f.getGenerate())
        assertEquals("Foo.kt", f.getFilename())
        assertEquals("C:/out/dir", f.getDirectory())
        assertEquals("dir/Foo.kt", f.finalFileRelativePath)
        assertEquals("dir/Foo.kt.ftl", f.templateFileRelativePath)
    }

    @Test
    fun settersAndPropertiesRoundTrip() {
        val f = newGenFile()
        f.setGenerate(true)
        f.setFilename("Bar.kt")
        f.setDirectory("C:/other")
        assertTrue(f.getGenerate())
        assertEquals("Bar.kt", f.getFilename())
        assertEquals("C:/other", f.getDirectory())

        assertTrue(f.generateProperty().get())
        assertEquals("Bar.kt", f.filenameProperty().get())
        assertEquals("C:/other", f.directoryProperty().get())

        f.filenameProperty().set("Baz.kt")
        assertEquals("Baz.kt", f.getFilename())
    }

    @Test
    fun mutableRelativePathFieldsAreReassignable() {
        val f = newGenFile()
        f.finalFileRelativePath = "new/Final.kt"
        f.templateFileRelativePath = "new/Template.ftl"
        assertEquals("new/Final.kt", f.finalFileRelativePath)
        assertEquals("new/Template.ftl", f.templateFileRelativePath)
    }

    @Test
    fun compareToOrdersByDirectory() {
        val a = newGenFile(directory = "C:/a")
        val b = newGenFile(directory = "C:/b")
        assertTrue(a.compareTo(b) < 0)
        assertTrue(b.compareTo(a) > 0)
        assertEquals(0, a.compareTo(newGenFile(directory = "C:/a", filename = "Other.kt")))
        assertEquals(0, a.compareTo(a))
    }

    @Test
    fun compareToGroupsByDirectoryIgnoringFilename() {
        val list = listOf(
            newGenFile(directory = "C:/z", filename = "A.kt"),
            newGenFile(directory = "C:/a", filename = "Z.kt"),
            newGenFile(directory = "C:/m", filename = "M.kt"),
        )
        assertEquals(listOf("C:/a", "C:/m", "C:/z"), list.sorted().map { it.getDirectory() })
    }

    @Test
    fun compareToHandlesWindowsBackslashPaths() {
        val a = newGenFile(directory = "C:\\proj\\a")
        val b = newGenFile(directory = "C:\\proj\\b")
        assertTrue(a.compareTo(b) < 0)
    }
}
