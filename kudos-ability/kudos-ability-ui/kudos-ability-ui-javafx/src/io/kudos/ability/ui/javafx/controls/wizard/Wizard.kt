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
 * 多步骤向导（Wizard）控件。
 *
 * 移植自 ControlsFX，按 [Flow] 决定页面流转顺序；维护 [pageHistory] 支持"上一步"，
 * 自带"上一步/下一步/完成"按钮。通过 [WizardPane] 子类自定义每一页内容并实现进入/离开回调。
 *
 * @param title 对话框标题
 * @author Oracle (原始)
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
    /** 各页填写结果的共享存储，向导完成后由调用方读取 */
    val settings: ObservableMap<String, in Any> = FXCollections.observableHashMap()
    private val pageHistory = Stack<WizardPane>()
    private var currentPage: Optional<WizardPane> = Optional.empty()

    //    private final ValidationSupport validationSupport = new ValidationSupport();
    //
    private val BUTTON_PREVIOUS = ButtonType("上一步", ButtonData.BACK_PREVIOUS)
    private val BUTTON_PREVIOUS_ACTION_HANDLER = EventHandler { actionEvent: ActionEvent ->
        actionEvent.consume()
        currentPage = Optional.ofNullable(pageHistory.takeUnless { it.isEmpty() }?.pop())
        updatePage(dialog, false)
    }
    private val BUTTON_NEXT = ButtonType("下一步", ButtonData.NEXT_FORWARD)
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
     * 非阻塞展示向导对话框。
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    fun show() {
        requireDialog().show()
    }

    /**
     * 阻塞展示向导对话框直到关闭。
     *
     * @return 用户点击的 [ButtonType]（取消时为 empty）
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    fun showAndWait(): Optional<ButtonType?> = requireDialog().showAndWait()

    /**
     * 取已初始化的 dialog；未初始化时抛 [IllegalArgumentException]，避免后续 NPE 难定位。
     *
     * @return 当前 dialog
     * @throws IllegalArgumentException dialog 未初始化时
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
     * be retrieved by calling [hello.dialog.wizard.Wizard.getUserData].
     *
     * @param value The value to be stored - this can later be retrieved by calling
     * [hello.dialog.wizard.Wizard.getUserData].
     */
    fun setUserData(value: Any) {
        requireNotNull(getProperties())[USER_DATA_KEY] = value
    }

    /**
     * Returns a previously set Object property, or null if no such property
     * has been set using the [hello.dialog.wizard.Wizard.setUserData] method.
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
     * 根据当前流程是否可继续 (`Flow.canAdvance`) 动态增删 Next / Finish 按钮。
     *
     * 把 Next 按钮插到 buttonTypes 列表首位让它成为默认按钮（回车响应），优先级高于 Cancel。
     * 注册 BUTTON_NEXT_ACTION_HANDLER 事件过滤器：拦截 Next 按钮的 ACTION 事件交由本类处理，
     * 避免 JavaFX Dialog 默认行为直接关闭对话框。
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

    /** 当前页内已记录的 setting 数量，用于给无 id 节点生成 `page_.setting_N` 形式的默认 key。 */
    private var settingCounter = 0

    /**
     * 把当前页上所有值型节点的值收集到 [settings] 里。
     *
     * 由于不知道 page 内部结构，从 page.content 起做 DFS 全遍历 [checkNode]，
     * 遇到 [io.kudos.ability.ui.javafx.controls.wizard.ValueExtractor] 能取值的节点就记下来。
     *
     * @param page 当前向导页
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
     * 深度优先遍历节点：当前节点能取值就记下并递归；不能就继续往下走。
     *
     * 注意 `fold(false) { acc, child -> checkNode(child) || acc }`——这里**不能**短路（用 `||` 在
     * acc 后面是关键），必须遍历每个孩子让所有 value-bearing 节点都被记录。
     *
     * @param n 当前节点；null 视为遍历到底
     * @return 当前子树是否记录到任何 setting
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
     * 试图从单个节点取值并存入 settings map：
     * - 节点带 id 用 id 作 key
     * - 无 id 退化为 `page_.setting_<counter>` 命名
     *
     * @param n 节点；null 直接返回 false
     * @return 是否成功取到值
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
     * 向导单页基类。继承本类并重写 [onEnteringPage]/[onExitingPage] 实现页面级回调。
     *
     * 注：目前还是基于 override 而非事件订阅；未来计划改为事件式 API。
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    // TODO this should just contain a ControlsFX Form, but for now it is hand-coded
    open class WizardPane : DialogPane() {
        /**
         * 进入本页时回调（点击"下一步"导航过来或首次进入）。
         * @param wizard 关联的 [Wizard]，可为 null
         */
        // TODO we want to change this to an event-based API eventually
        open fun onEnteringPage(wizard: Wizard?) {}

        /**
         * 离开本页时回调（点击"上一步/下一步"导航走时）。
         * 注意 Wizard 控件有"从 N+1 回到 N 时再触发一次 N-1 的 onExitingPage"的已知 bug，
         * 调用方需在子类内部用 try/catch 兜底。
         * @param wizard 关联的 [Wizard]，可为 null
         */
        // TODO same here - replace with events
        open fun onExitingPage(wizard: Wizard?) {}
    }

    /**
     * 向导流程控制器：决定"哪一页 → 下一页"以及"能否继续"。
     * 框架提供 [LinearWizardFlow] 实现线性顺序；分支场景下用户自行实现。
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    interface Flow {
        /**
         * 由当前页推导出下一页。
         * @param currentPage 当前页，可为 null（首次进入）
         * @return 下一页；返回 empty 表示流程结束
         */
        fun advance(currentPage: WizardPane?): Optional<WizardPane>

        /**
         * 判断"下一步"按钮是否应可点。
         * @param currentPage 当前页
         * @return true 表示允许前进
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
