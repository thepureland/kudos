package io.kudos.tools.codegen.fx.ui

import io.kudos.tools.codegen.core.TemplateModelCreator
import kotlin.test.*

/**
 * test for SingleTableCodeGenerateWizard
 *
 * Covers the no-arg construction and the open extension point getTemplateModelCreator() (default and the
 * documented subclass-override scenario).
 *
 * Not covered (recorded as uncovered): start(Stage) — it loads FXML, builds a three-page Wizard and calls
 * showAndWait(), which blocks the FX application thread waiting for real user interaction and cannot be
 * exercised headlessly/deterministically.
 *
 * @author K
 * @since 1.0.0
 */
internal class SingleTableCodeGenerateWizardTest {

    @Test
    fun defaultTemplateModelCreatorIsTemplateModelCreator() {
        val wizard = SingleTableCodeGenerateWizard()
        assertIs<TemplateModelCreator>(wizard.getTemplateModelCreator())
    }

    @Test
    fun subclassCanOverrideTemplateModelCreator() {
        val custom = object : TemplateModelCreator() {}
        val wizard = object : SingleTableCodeGenerateWizard() {
            override fun getTemplateModelCreator(): TemplateModelCreator = custom
        }
        assertSame(custom, wizard.getTemplateModelCreator())
    }
}
