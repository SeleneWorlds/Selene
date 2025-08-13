package world.selene.client.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.I18NBundle
import com.github.czyzby.lml.vis.util.VisLml
import world.selene.client.assets.BundleFileResolver
import world.selene.client.ui.UI
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkJavaObject
import world.selene.common.lua.checkString
import world.selene.common.lua.register

class LuaUIModule(private val ui: UI, private val bundleFileResolver: BundleFileResolver) : LuaModule {
    override val name = "selene.ui.lml"

    override fun initialize(luaManager: LuaManager) {
        luaManager.exposeClass(ActorLuaProxy::class)
        luaManager.exposeClass(SkinLuaProxy::class)
    }

    override fun register(table: LuaValue) {
        table.register("LoadUI", this::luaLoadUI)
        table.register("LoadSkin", this::luaLoadSkin)
        table.register("CreateSkin", this::luaCreateSkin)
        table.set("Root", ActorLuaProxy(ui.bundlesRoot))
    }

    private fun luaLoadUI(lua: Lua): Int {
        val xmlFilePath = lua.checkString(1)

        val actions = mutableMapOf<String, LuaValue>()
        var i18nBundle = "system"
        var skin: Skin? = null

        // Load options from second parameter
        if (lua.isTable(2)) {
            lua.getField(2, "actions")
            if (lua.isTable(-1)) {
                lua.toMap(-1)?.entries?.forEach { (actionName, actionFunction) ->
                    if (actionName is String && actionFunction is LuaValue) {
                        actions[actionName] = actionFunction
                    }
                }
            }
            lua.pop(1)

            lua.getField(2, "i18nBundle")
            if (lua.isString(-1)) {
                i18nBundle = lua.toString(-1) ?: "system"
            }
            lua.pop(1)

            lua.getField(2, "skin")
            if (lua.isUserdata(-1)) {
                skin = lua.checkJavaObject(-1, SkinLuaProxy::class)?.delegate
            }
            lua.pop(1)
        }

        try {
            val parser = VisLml.parser().skin(skin ?: ui.systemSkin)

            // Register actions from Lua
            for ((actionName, actionFunction) in actions) {
                parser.action(actionName) { actor ->
                    try {
                        actionFunction.call(ActorLuaProxy(actor as Actor))
                    } catch (e: Exception) {
                        // Log error but don't crash the UI
                        println("Error executing Lua action '$actionName': ${e.message}")
                    }
                }
            }

            // Set up i18n bundle
            val i18nFileHandle = bundleFileResolver.resolve(i18nBundle)
            val i18nBundle = I18NBundle.createBundle(i18nFileHandle)
            parser.i18nBundle(i18nBundle)

            // Parse the UI XML file
            val xmlFile = bundleFileResolver.resolve(xmlFilePath)
            if (!xmlFile.exists()) {
                return lua.error(IllegalArgumentException("XML file not found: $xmlFilePath"))
            }

            val actors = parser.build().parseTemplate(xmlFile)

            val actorsByName = mutableMapOf<String, ActorLuaProxy>()
            fun collectActorsByName(actor: Actor) {
                if (actor.name != null && actor.name.isNotEmpty()) {
                    actorsByName[actor.name] = ActorLuaProxy(actor)
                }
                if (actor is Group) {
                    actor.children.forEach { child ->
                        collectActorsByName(child)
                    }
                }
            }
            actors.forEach { collectActorsByName(it) }
            
            lua.push( actors.map { ActorLuaProxy(it) }, Lua.Conversion.FULL)
            lua.push(actorsByName, Lua.Conversion.FULL)
            return 2
        } catch (e: Exception) {
            return lua.error(e)
        }
    }

    private fun luaLoadSkin(lua: Lua): Int {
        val skinPath = lua.checkString(1)

        try {
            val skinFile = bundleFileResolver.resolve(skinPath)
            if (!skinFile.exists()) {
                return lua.error(IllegalArgumentException("Skin file not found: $skinPath"))
            }

            val skin = Skin(skinFile)
            lua.push(SkinLuaProxy(skin, bundleFileResolver), Lua.Conversion.NONE)
            return 1
        } catch (e: Exception) {
            return lua.error(e)
        }
    }

    private fun luaCreateSkin(lua: Lua): Int {
        val font = BitmapFont()
        val skin = Skin().apply {
            add("default", font)
            add("default", Label.LabelStyle(font, Color.WHITE))
        }
        lua.push(SkinLuaProxy(skin, bundleFileResolver), Lua.Conversion.NONE)
        return 1
    }

    class ActorLuaProxy(private val delegate: Actor) {
        fun AddChild(child: ActorLuaProxy) {
            when (delegate) {
                is Container<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    (delegate as Container<Actor>).actor = child.delegate
                }

                is Group -> {
                    delegate.addActor(child.delegate)
                }

                else -> {
                    throw IllegalArgumentException("Actor of type ${delegate.javaClass.simpleName} cannot have children")
                }
            }
        }
    }

    class SkinLuaProxy(val delegate: Skin, private val bundleFileResolver: BundleFileResolver) {
        fun AddTexture(lua: Lua): Int {
            val name = lua.checkString(2)
            val texturePath = lua.checkString(3)

            try {
                val textureFile = bundleFileResolver.resolve(texturePath)
                if (!textureFile.exists()) {
                    return lua.error(IllegalArgumentException("Texture file not found: $texturePath"))
                }

                val texture = Texture(textureFile)
                val region = TextureRegion(texture)
                delegate.add(name, region)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to add texture region '$name': ${e.message}", e))
            }
        }

        fun AddButtonStyle(lua: Lua): Int {
            val styleName = lua.checkString(2)

            try {
                val buttonStyle = Button.ButtonStyle()

                if (lua.isTable(2)) {
                    // Get up texture
                    lua.getField(2, "up")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        buttonStyle.up = resolveDrawable(path)
                    }
                    lua.pop(1)

                    // Get down texture
                    lua.getField(2, "down")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        buttonStyle.down = resolveDrawable(path)
                    }
                    lua.pop(1)

                    // Get over texture
                    lua.getField(2, "over")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        buttonStyle.over = resolveDrawable(path)
                    }
                    lua.pop(1)
                }

                delegate.add(styleName, buttonStyle)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to add button style '$styleName': ${e.message}", e))
            }
        }

        fun AddLabelStyle(lua: Lua): Int {
            val styleName = lua.checkString(2)

            try {
                val labelStyle = Label.LabelStyle()

                if (lua.isTable(3)) {
                    lua.getField(3, "font")
                    if (lua.isString(-1)) {
                        val fontName = lua.toString(-1)!!
                        labelStyle.font = resolveFont(fontName)
                    }
                    lua.pop(1)

                    lua.getField(3, "fontColor")
                    if (lua.isString(-1)) {
                        val colorString = lua.toString(-1)!!
                        labelStyle.fontColor = resolveColor(colorString)
                    }
                    lua.pop(1)

                    lua.getField(3, "background")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        labelStyle.background = resolveDrawable(path)
                    }
                    lua.pop(1)
                }

                delegate.add(styleName, labelStyle)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to add label style '$styleName': ${e.message}", e))
            }
        }

        private fun resolveFont(fontName: String): BitmapFont {
            delegate.optional(fontName, BitmapFont::class.java)?.let { return it }
            
            val fontFile = bundleFileResolver.resolve(fontName)
            if (fontFile.exists()) {
                return BitmapFont(fontFile)
            } else {
                throw IllegalArgumentException("Font not found in skin or file system: $fontName")
            }
        }

        private fun resolveDrawable(path: String): Drawable? {
            delegate.optional(path, TextureRegion::class.java)?.let {
                return TextureRegionDrawable(it) 
            }
            
            val textureFile = bundleFileResolver.resolve(path)
            return if (textureFile.exists()) {
                TextureRegionDrawable(TextureRegion(Texture(textureFile)))
            } else {
                null
            }
        }

        private fun resolveColor(colorString: String): Color {
            return when {
                colorString.startsWith("#") -> {
                    try {
                        val hex = colorString.substring(1)
                        when (hex.length) {
                            6 -> Color.valueOf(hex + "FF")
                            8 -> Color.valueOf(hex)
                            else -> throw IllegalArgumentException("Invalid hex color: $colorString")
                        }
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Invalid hex color: $colorString", e)
                    }
                }

                else -> {
                    delegate.optional(colorString, Color::class.java)
                        ?: throw IllegalArgumentException("Color not found in skin: $colorString")
                }
            }
        }
    }
}