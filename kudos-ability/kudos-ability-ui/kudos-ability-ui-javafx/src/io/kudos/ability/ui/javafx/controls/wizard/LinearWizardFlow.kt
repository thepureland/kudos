/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package io.kudos.ability.ui.javafx.controls.wizard

import java.util.ArrayList
import java.util.Optional

/**
 * Linear [Wizard.Flow] implementation: advances serially in the order given at construction.
 * Most "config -> options -> finish" style wizards can reuse this implementation directly.
 *
 * @param pages Page collection with a fixed order; null is treated as an empty list
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class LinearWizardFlow(pages: Collection<Wizard.WizardPane?>?) : Wizard.Flow {

    /** Internal immutable copy, so external mutation of the original collection cannot affect the wizard flow. */
    private val pages: List<Wizard.WizardPane?>

    /**
     * Convenience constructor: pass pages directly in vararg form.
     *
     * @param pages Ordered list of pages
     * @author K
     * @since 1.0.0
     */
    constructor(vararg pages: Wizard.WizardPane) : this(listOf(*pages))

    /**
     * Advance to the next page: take the current page's position in the list + 1.
     *
     * @param currentPage Current page (null is treated as index -1, advancing to the first page)
     * @return The next page; if already on the last page, throws [IndexOutOfBoundsException] --
     *         [canAdvance] is expected to guard this.
     * @author K
     * @since 1.0.0
     */
    override fun advance(currentPage: Wizard.WizardPane?): Optional<Wizard.WizardPane> {
        var pageIndex = pages.indexOf(currentPage)
        return Optional.ofNullable(pages[++pageIndex])
    }

    /**
     * Tell whether there is still a next page.
     *
     * @param currentPage Current page
     * @return true if more pages are still available after it
     * @author K
     * @since 1.0.0
     */
    override fun canAdvance(currentPage: Wizard.WizardPane?): Boolean {
        val pageIndex = pages.indexOf(currentPage)
        return pages.size - 1 > pageIndex
    }

    init {
        this.pages = ArrayList(pages.orEmpty())
    }
}
