package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.seleneworlds.common.lua.util.checkFloat
import com.seleneworlds.common.lua.util.checkFunction
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.lua.util.xpCall
import com.seleneworlds.common.script.ConstantTrace
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

object SliderLuaMetatable {
    val luaMeta = ProgressBarLuaMetatable.luaMeta.extend(Slider::class) {
        callable(::getMinValue)
        callable(::setMinValue)
        callable(::getMaxValue)
        callable(::setMaxValue)
        callable(::setRange)
        callable(::getStepSize)
        callable(::setStepSize)
        callable(::onChanged)
    }

    private fun getMinValue(lua: Lua): Int {
        val slider = lua.checkUserdata<Slider>(1)
        lua.push(slider.minValue)
        return 1
    }

    private fun setMinValue(lua: Lua): Int {
        val slider = lua.checkUserdata<Slider>(1)
        val min = lua.checkFloat(2)
        slider.setRange(min, slider.maxValue)
        return 0
    }

    private fun getMaxValue(lua: Lua): Int {
        val slider = lua.checkUserdata<Slider>(1)
        lua.push(slider.maxValue)
        return 1
    }

    private fun setMaxValue(lua: Lua): Int {
        val slider = lua.checkUserdata<Slider>(1)
        val max = lua.checkFloat(2)
        slider.setRange(slider.minValue, max)
        return 0
    }

    private fun setRange(lua: Lua): Int {
        val slider = lua.checkUserdata<Slider>(1)
        val min = lua.checkFloat(2)
        val max = lua.checkFloat(3)
        slider.setRange(min, max)
        return 0
    }

    private fun getStepSize(lua: Lua): Int {
        val slider = lua.checkUserdata<Slider>(1)
        lua.push(slider.stepSize)
        return 1
    }

    private fun setStepSize(lua: Lua): Int {
        val slider = lua.checkUserdata<Slider>(1)
        val stepSize = lua.checkFloat(2)
        slider.stepSize = stepSize
        return 0
    }

    private fun onChanged(lua: Lua): Int {
        val slider = lua.checkUserdata<Slider>(1)
        val callback = lua.checkFunction(2)
        val trace = ConstantTrace("[slider onChanged] registered in ${lua.getCallerInfo()}")

        slider.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                runCallback(callback, trace, slider)
            }
        })
        return 0
    }

    private fun runCallback(callback: LuaValue, trace: ConstantTrace, slider: Slider) {
        val callbackLua = callback.state()
        callbackLua.push(callback)
        callbackLua.push(slider, Lua.Conversion.NONE)
        callbackLua.push(slider.value)
        try {
            callbackLua.xpCall(2, 0, trace)
        } catch (e: LuaException) {
            logger.error("Lua error in slider change callback", e)
        }
    }

    private val logger = LoggerFactory.getLogger(SliderLuaMetatable::class.java)
}
