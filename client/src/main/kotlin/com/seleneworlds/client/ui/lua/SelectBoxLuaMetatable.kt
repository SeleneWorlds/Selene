package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Array
import com.seleneworlds.client.ui.BundleUiListeners
import com.seleneworlds.common.bundles.BundleExecutionContext
import com.seleneworlds.common.lua.util.checkFunction
import com.seleneworlds.common.lua.util.checkInt
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.lua.util.throwError
import com.seleneworlds.common.lua.util.toAny
import com.seleneworlds.common.lua.util.xpCall
import com.seleneworlds.common.script.ConstantTrace
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

object SelectBoxLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(SelectBox::class) {
        callable(::clearItems)
        callable(::setItems)
        callable(::getSelectedIndex)
        callable(::setSelectedIndex)
        callable(::getSelectedValue)
        callable(::setSelectedValue)
        callable(::getSelectedId)
        callable(::setSelectedId)
        callable(::onChanged)
    }

    private fun clearItems(lua: Lua): Int {
        val selectBox = lua.checkUserdata<SelectBox<Any>>(1)
        selectBox.clearItems()
        return 0
    }

    private fun setItems(lua: Lua): Int {
        val selectBox = lua.checkUserdata<SelectBox<Any>>(1)
        val items = lua.toList(2) ?: lua.throwError("Expected a Lua list of select box items")

        val selectBoxItems = Array<Any>()
        items.forEachIndexed { index, item ->
            selectBoxItems.add(toSelectBoxItem(lua, item, index + 1))
        }
        selectBox.setItems(selectBoxItems)
        return 0
    }

    private fun getSelectedIndex(lua: Lua): Int {
        val selectBox = lua.checkUserdata<SelectBox<Any>>(1)
        val selectedIndex = selectBox.selectedIndex
        lua.push(selectedIndex + 1)
        return 1
    }

    private fun setSelectedIndex(lua: Lua): Int {
        val selectBox = lua.checkUserdata<SelectBox<Any>>(1)
        val index = if (lua.isNil(2)) 0 else lua.checkInt(2)
        if (index <= 0) {
            selectBox.selected = null
            return 0
        }

        val javaIndex = index - 1
        if (javaIndex >= selectBox.items.size) {
            lua.throwError("Select box index $index is out of bounds")
        }
        selectBox.selectedIndex = javaIndex
        return 0
    }

    private fun getSelectedValue(lua: Lua): Int {
        val selectBox = lua.checkUserdata<SelectBox<Any>>(1)
        lua.pushLuaValue(selectBox.selected?.selectBoxValue)
        return 1
    }

    private fun setSelectedValue(lua: Lua): Int {
        val selectBox = lua.checkUserdata<SelectBox<Any>>(1)
        val value = lua.toAny(2)
        if (value == null) {
            selectBox.selected = null
            return 0
        }

        selectBox.selected = selectBox.items.firstOrNull { it.selectBoxValue == value }
            ?: lua.throwError("Select box value was not found")
        return 0
    }

    private fun getSelectedId(lua: Lua): Int {
        val selectBox = lua.checkUserdata<SelectBox<Any>>(1)
        lua.pushLuaValue(selectBox.selected?.selectBoxId)
        return 1
    }

    private fun setSelectedId(lua: Lua): Int {
        val selectBox = lua.checkUserdata<SelectBox<Any>>(1)
        val id = lua.toAny(2)
        if (id == null) {
            selectBox.selected = null
            return 0
        }

        selectBox.selected = selectBox.items.firstOrNull { it.selectBoxId == id }
            ?: lua.throwError("Select box id was not found")
        return 0
    }

    private fun onChanged(lua: Lua): Int {
        val selectBox = lua.checkUserdata<SelectBox<Any>>(1)
        val callback = lua.checkFunction(2)
        val trace = ConstantTrace("[selectBox onChanged] registered in ${lua.getCallerInfo()}")
        val bundle = BundleExecutionContext.currentBundle

        BundleUiListeners.addActorListener(selectBox, object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                BundleUiListeners.runInBundleContext(bundle) {
                    runCallback(callback, trace, selectBox)
                }
            }
        }, bundle)
        return 0
    }

    private fun runCallback(callback: LuaValue, trace: ConstantTrace, selectBox: SelectBox<Any>) {
        val callbackLua = callback.state()
        callbackLua.push(callback)
        callbackLua.push(selectBox, Lua.Conversion.NONE)
        callbackLua.push(selectBox.selectedIndex + 1)
        callbackLua.pushLuaValue(selectBox.selected?.selectBoxValue)
        try {
            callbackLua.xpCall(3, 0, trace)
        } catch (e: LuaException) {
            logger.error("Lua error in select box change callback", e)
        }
    }

    private fun toSelectBoxItem(lua: Lua, item: Any?, index: Int): SelectBoxItem {
        return when (item) {
            is Map<*, *> -> {
                val label = item["label"] ?: item["text"] ?: item["name"]
                    ?: lua.throwError("Select box item at index $index needs a label")
                SelectBoxItem(
                    label = label.toString(),
                    value = item["value"] ?: item["id"] ?: label,
                    id = item["id"]
                )
            }

            else -> SelectBoxItem(
                label = item?.toString() ?: "",
                value = item,
                id = null
            )
        }
    }

    private val Any.selectBoxValue: Any
        get() = (this as? SelectBoxItem)?.value ?: this

    private val Any.selectBoxId: Any?
        get() = (this as? SelectBoxItem)?.id

    private fun Lua.pushLuaValue(value: Any?) {
        when (value) {
            null -> pushNil()
            is LuaValue -> push(value, Lua.Conversion.NONE)
            else -> push(value, Lua.Conversion.FULL)
        }
    }

    private data class SelectBoxItem(
        val label: String,
        val value: Any?,
        val id: Any?
    ) {
        override fun toString(): String = label
    }

    private val logger = LoggerFactory.getLogger(SelectBoxLuaMetatable::class.java)
}
