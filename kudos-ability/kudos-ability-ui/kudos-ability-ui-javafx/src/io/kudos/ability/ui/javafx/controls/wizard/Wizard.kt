/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.DialogPane
import java.util.Optional
import java.util.Stack

/**
 * Multi-step wizard control.
 *
 * Ported from ControlsFX; the [Flow] determines page navigation order. Maintains [pageHistory]
 * to support "previous", and provides built-in "previous / next / finish" buttons. Subclass
 * [WizardPane] to customize each page's content and implement enter/exit callbacks.
 *
 * @param title Dialog title
 * @author Oracle (original)
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class Wizard(title: String = "") {
    /**************************************************************************
     *
     * Static fields
     *
     */
    /**************************************************************************
     *
     * Private fields
     *
     */
    private var dialog: Dialog<ButtonType?>? = null

    // --- settings
    /** Shared storage for values filled in across pages; read by the caller after the wizard completes. */
    val settings: ObservableMap<String, in Any> = FXCollections.observableHashMap()
    private val pageHistory = Stack<WizardPane>()
    private var currentPage: Optional<WizardPane> = Optional.empty()

    //    private final ValidationSupport validationSupport = new ValidationSupport();
    //
    private val BUTTON_PREVIOUS = ButtonType("Previous", ButtonData.BACK_PREVIOUS)
    private val BUTTON_PREVIOUS_ACTION_HANDLER = EventHandler { actionEvent: ActionEvent ->
        actionEvent.consume()
        currentPage = Optional.ofNullable(pageHistory.takeUnless { it.isEmpty() }?.pop())
        updatePage(dialog, false)
    }
    private val BUTTON_NEXT = ButtonType("Next", ButtonData.NEXT_FORWARD)
    private val BUTTON_NEXT_ACTION_HANDLER = EventHandler { actionEvent: ActionEvent ->
        actionEvent.consume()
        currentPage.ifPresent { pageHistory.push(it) }
        currentPage = getFlow().advance(currentPage.orElse(null))
        updatePage(dialog, true)
    }
    /**************************************************************************
     *
     * Constructors
     *
     */
    /**
     *
     */
    constructor() : this("")

    /**************************************************************************
     *
     * Public API
     *
     */
    /**
     * Show the wizard dialog non-blocking.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    fun show() {
        requireDialog().show()
    }

    /**
     * Show the wizard dialog blocking until it is closed.
     *
     * @return The [ButtonType] the user clicked (empty on cancel)
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    fun showAndWait(): Optional<ButtonType?> = requireDialog().showAndWait()

    /**
     * Get the initialized dialog; throws [IllegalArgumentException] when it has not yet been
     * initialized, to avoid a later NPE that is hard to trace.
     *
     * @return The current dialog
     * @throws IllegalArgumentException when the dialog has not been initialized
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private fun requireDialog(): Dialog<ButtonType?> = requireNotNull(dialog) { "dialog is null" }

    /**************************************************************************
     *
     * Properties
     *
     */
    // --- flow
    private val flow = object : SimpleObjectProperty<Flow>(
        LinearWizardFlow()
    ) {
        override fun invalidated() {
            updatePage(dialog, false)
        }

        override fun set(flow: Flow) {
            super.set(flow)
            pageHistory.clear()
            currentPage = flow.advance(currentPage.orElse(null))
            updatePage(dialog, true)
        }
    }

    fun flowProperty(): ObjectProperty<Flow> = flow

    fun getFlow(): Flow = flow.get()

    fun setFlow(flow: Flow) {
        this.flow.set(flow)
    }

    // A map containing a set of properties for this Wizard
    private var properties: ObservableMap<Any, Any>? = null

    /**
     * Returns an observable map of properties on this Wizard for use primarily
     * by application developers.
     *
     * @return an observable map of properties on this Wizard for use primarily
     * by application developers
     */
    fun getProperties(): ObservableMap<Any, Any>? {
        return properties ?: FXCollections.observableMap(mutableMapOf<Any, Any>()).also { properties = it }
    }

    /**
     * Tests if this Wizard has properties.
     * @return true if this Wizard has properties.
     */
    fun hasProperties(): Boolean = !properties.isNullOrEmpty()

    // --- UserData
    /**
     * Convenience method for setting a single Object property that can be
     * retrieved at a later date. This is functionally equivalent to calling
     * the getProperties().put(Object key, Object value) method. This can later
     * be retrieved by reading [userData].
     *
     * @param value The value to be stored - this can later be retrieved by reading
     * [userData].
     */
    fun setUserData(value: Any) {
        requireNotNull(getProperties())[USER_DATA_KEY] = value
    }

    /**
     * Returns a previously set Object property, or null if no such property
     * has been set using the [setUserData] method.
     *
     * @return The Object that was previously set, or null if no property
     * has been set or if null was set.
     */
    val userData: Any?
        get() = getProperties()?.get(USER_DATA_KEY)
    //    public ValidationSupport getValidationSupport() {
    //      return validationSupport;
    //  }
    /**************************************************************************
     *
     * Private implementation
     *
     */
    private fun updatePage(dialog: Dialog<ButtonType?>?, advancing: Boolean) {
        pageHistory.takeUnless { it.isEmpty() }?.peek()?.let { page ->
            // if we are going forward in the wizard, we read in the settings
            // from the page and store them in the settings map.
            // If we are going backwards, we do nothing
            if (advancing) readSettings(page)

            // give the previous wizard page a chance to update the pages list
            // based on the settings it has received
            page.onExitingPage(this)
        }
        currentPage.ifPresent { page ->
            // put in default actions
            val buttons = page.buttonTypes
            if (!buttons.contains(BUTTON_PREVIOUS)) {
                buttons.add(BUTTON_PREVIOUS)
                (page.lookupButton(BUTTON_PREVIOUS) as Button)
                    .addEventFilter(ActionEvent.ACTION, BUTTON_PREVIOUS_ACTION_HANDLER)
            }
            if (!buttons.contains(BUTTON_NEXT)) {
                buttons.add(BUTTON_NEXT)
                (page.lookupButton(BUTTON_NEXT) as Button)
                    .addEventFilter(ActionEvent.ACTION, BUTTON_NEXT_ACTION_HANDLER)
            }
            if (!buttons.contains(ButtonType.FINISH)) buttons.add(ButtonType.FINISH)
            if (!buttons.contains(ButtonType.CANCEL)) buttons.add(ButtonType.CANCEL)

            // then give user a chance to modify the default actions
            page.onEnteringPage(this)

            // and then switch to the new pane
            requireNotNull(dialog) { "dialog is null" }.dialogPane = page
        }
        validateActionState()
    }

    /**
     * Dynamically add/remove the Next / Finish buttons based on whether the current flow can
     * advance (`Flow.canAdvance`).
     *
     * Inserts the Next button at the head of buttonTypes so it becomes the default button
     * (responds to Enter), taking priority over Cancel. Registers BUTTON_NEXT_ACTION_HANDLER as
     * an event filter: it intercepts the Next button's ACTION event and lets this class handle it,
     * preventing JavaFX Dialog's default behavior of closing the dialog.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private fun validateActionState() {
        val pane = requireDialog().dialogPane
        val currentPaneButtons = pane.buttonTypes

        // TODO can't set a DialogButton to be disabled at present
//        BUTTON_PREVIOUS.setDisabled(pageHistory.isEmpty());

        // Note that we put the 'next' and 'finish' actions at the beginning of
        // the actions list, so that it takes precedence as the default button,
        // over, say, cancel. We will probably want to handle this better in the
        // future...
        if (!getFlow().canAdvance(currentPage.orElse(null))) {
            currentPaneButtons.remove(BUTTON_NEXT)
        } else {
            if (currentPaneButtons.contains(BUTTON_NEXT)) {
                currentPaneButtons.remove(BUTTON_NEXT)
                currentPaneButtons.add(0, BUTTON_NEXT)
                (pane.lookupButton(BUTTON_NEXT) as Button)
                    .addEventFilter(ActionEvent.ACTION, BUTTON_NEXT_ACTION_HANDLER)
            }
            currentPaneButtons.remove(ButtonType.FINISH)
        }
    }

    /** Number of settings already recorded on the current page; used to generate default keys of the form `page_.setting_N` for nodes without an id. */
    private var settingCounter = 0

    /**
     * Collect values from all value-bearing nodes on the current page into [settings].
     *
     * Since the page's internal structure is unknown, do a full DFS [checkNode] starting from
     * page.content, recording every node from which
     * [io.kudos.ability.ui.javafx.controls.wizard.ValueExtractor] can read a value.
     *
     * @param page The current wizard page
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private fun readSettings(page: WizardPane) {
        // for now, we cannot know the structure of the page, so we just drill down
        // through the entire scenegraph (from page.content down) until we get
        // to the leaf nodes. We stop only if we find a node that is a
        // ValueContainer (either by implementing the interface), or being
        // listed in the internal valueContainers map.
        settingCounter = 0
        checkNode(page.content)
    }

    /**
     * Depth-first node traversal: record this node if a value can be extracted, then recurse;
     * otherwise continue downward.
     *
     * Note the `fold(false) { acc, child -> checkNode(child) || acc }` -- short-circuiting
     * **must not** happen here (putting `||` after acc is the key); every child must be visited
     * so every value-bearing node gets recorded.
     *
     * @param n The current node; null is treated as the end of traversal
     * @return Whether this subtree recorded any setting
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private fun checkNode(n: Node?): Boolean {
        if (readSetting(n)) return true
        // we're doing a depth-first search; visit every child so that all
        // value-bearing nodes get recorded (don't short-circuit on first hit)
        return ImplUtils.getChildren(n).fold(false) { acc, child -> checkNode(child) || acc }
    }

    /**
     * Try to extract a value from a single node and store it in the settings map:
     * - if the node has an id, use the id as the key
     * - otherwise fall back to `page_.setting_<counter>` naming
     *
     * @param n The node; null returns false immediately
     * @return Whether a value was successfully extracted
     * @author K
     * @since 1.0.0
     */
    private fun readSetting(n: Node?): Boolean {
        if (n == null) return false
        val setting = ValueExtractor.getValue(n) ?: return false
        // save it into the settings map.
        // if the node has an id set, we will use that as the setting name, otherwise
        // fall back to a generic name based on the setting counter
        val settingName = n.id?.takeUnless { it.isEmpty() } ?: "page_.setting_$settingCounter"
        settings[settingName] = setting
        settingCounter++
        return true
    }
    /**************************************************************************
     *
     * Support classes
     *
     */
    /**
     *
     */
    /**
     * Base class for a single wizard page. Subclass and override
     * [onEnteringPage]/[onExitingPage] to implement page-level callbacks.
     *
     * Note: currently based on overrides rather than event subscription; planned to switch to an
     * event-based API in the future.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    // TODO this should just contain a ControlsFX Form, but for now it is hand-coded
    open class WizardPane : DialogPane() {
        /**
         * Callback invoked when this page is entered (navigated to via "Next" or on first entry).
         * @param wizard The associated [Wizard]; may be null
         */
        // TODO we want to change this to an event-based API eventually
        open fun onEnteringPage(wizard: Wizard?) {}

        /**
         * Callback invoked when leaving this page (navigated away via "Previous" or "Next").
         * Note the Wizard control has a known bug where going back from N+1 to N also re-triggers
         * onExitingPage for N-1; the caller must guard against this with try/catch in the subclass.
         * @param wizard The associated [Wizard]; may be null
         */
        // TODO same here - replace with events
        open fun onExitingPage(wizard: Wizard?) {}
    }

    /**
     * Wizard flow controller: decides "which page -> next page" and "whether it can advance".
     * The framework ships [LinearWizardFlow] for linear sequencing; users implement their own for
     * branching scenarios.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    interface Flow {
        /**
         * Derive the next page from the current one.
         * @param currentPage The current page; may be null (first entry)
         * @return The next page; returning empty signals end of flow
         */
        fun advance(currentPage: WizardPane?): Optional<WizardPane>

        /**
         * Tell whether the "Next" button should be clickable.
         * @param currentPage The current page
         * @return true to allow advancing
         */
        fun canAdvance(currentPage: WizardPane?): Boolean
    }

    companion object {
        // --- Properties
        private val USER_DATA_KEY = Any()
    }
    /**
     *
     * @param owner
     * @param title
     */
    /**
     *
     * @param owner
     */
    init {
//        validationSupport.validationResultProperty().addListener( (o, ov, nv) -> validateActionState());
        dialog = Dialog<ButtonType?>().also { it.title = title }
        //        hello.dialog.initOwner(owner); // TODO add initOwner API
    }
}
