package io.kudos.tools.codegen.core

import freemarker.template.Configuration
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Parses every packaged template with FreeMarker.
 *
 * The generator only touches a template when someone runs the JavaFX wizard against a live database,
 * so a malformed directive stays invisible until the next person generates a module — and then fails
 * in a UI, far from whoever broke it. This test turns that into a build failure.
 *
 * It parses only; it does not render. Undefined macros such as `generateClassComment` (supplied at
 * render time by the auto-included `macro.include`) and undefined variables are resolved during
 * rendering, not parsing, so they are correctly not errors here. What it does catch is the class of
 * mistake that actually happens when editing these files by hand: an unclosed `<#if>` / `<#list>`,
 * a stray `<#--`, or a malformed `${...}`.
 *
 * **A convention this cannot check**: `<#-- ... -->` is stripped before output, so anything written
 * that way never reaches the generated file. Use it only for notes about template logic (explaining
 * an adjacent `<#if>` / `<#assign>`); notes meant for whoever reads the *generated* code must be
 * plain `//` Kotlin comments.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class TemplateSyntaxTest {

    @Test
    fun everyTemplateParses() {
        val root = File("resources/templates")
        assertTrue(root.isDirectory, "Template root not found at ${root.absolutePath}")

        val templates = root.walkTopDown().filter { it.isFile }.toList()
        assertTrue(templates.size > 20, "Expected the template tree to be found, got ${templates.size} files")

        val cfg = Configuration(Configuration.VERSION_2_3_30).apply { defaultEncoding = "UTF-8" }
        val failures = templates.mapNotNull { file ->
            runCatching {
                freemarker.template.Template(
                    file.relativeTo(root).path, file.readText(Charsets.UTF_8), cfg
                )
            }.exceptionOrNull()?.let { "${file.relativeTo(root).path}: ${it.message?.lineSequence()?.first()}" }
        }
        if (failures.isNotEmpty()) {
            fail("${failures.size} template(s) failed to parse:\n" + failures.joinToString("\n"))
        }
    }

}
