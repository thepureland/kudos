package io.kudos.tools.codegen.fx.ui

import io.kudos.tools.codegen.core.TemplateModelCreator
import kotlin.test.*

/**
 * test for MultiTablesCodeGenerateWizard
 *
 * Covers the no-arg construction and the open extension point getTemplateModelCreator() (default and the
 * documented subclass-override scenario).
 *
 * Not covered (recorded as uncovered): start(Stage) — it loads FXML, builds a two-page Wizard and calls
 * showAndWait(), which blocks the FX application thread waiting for real user interaction and cannot be
 * exercised headlessly/deterministically.
 *
 * @author K
 * @since 1.0.0
 */
internal class MultiTablesCodeGenerateWizardTest {

    @Test
    fun defaultTemplateModelCreatorIsTemplateModelCreator() {
        val wizard = MultiTablesCodeGenerateWizard()
        assertIs<TemplateModelCreator>(wizard.getTemplateModelCreator())
    }

    @Test
    fun subclassCanOverrideTemplateModelCreator() {
        val custom = object : TemplateModelCreator() {}
        val wizard = object : MultiTablesCodeGenerateWizard() {
            override fun getTemplateModelCreator(): TemplateModelCreator = custom
        }
        assertSame(custom, wizard.getTemplateModelCreator())
    }
}
