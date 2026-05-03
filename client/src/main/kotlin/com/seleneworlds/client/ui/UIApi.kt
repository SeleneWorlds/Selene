package com.seleneworlds.client.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.I18NBundle
import com.kotcrab.vis.ui.widget.Draggable
import com.seleneworlds.client.assets.AssetProvider
import com.seleneworlds.client.bundles.BundleFileResolver
import com.seleneworlds.client.ui.lml.SeleneLmlParser
import java.util.concurrent.CompletableFuture

/**
 * Load, skin and manipulate UIs.
 */
class UIApi(
    private val ui: UI,
    private val bundleFileResolver: BundleFileResolver,
    val skinResolvers: SkinResolvers,
    private val assetProvider: AssetProvider
) {
    val bundlesRoot: Stack = ui.bundlesRoot

    fun addInputProcessor(
        keyUp: ((InputEvent, Int) -> Boolean)?,
        keyDown: ((InputEvent, Int) -> Boolean)?,
        keyTyped: ((InputEvent, Char) -> Boolean)?
    ) {
        ui.stage.addListener { event ->
            if (event is InputEvent) {
                return@addListener when (event.type) {
                    InputEvent.Type.keyUp -> {
                        if (keyUp != null) {
                            keyUp(event, event.keyCode)
                        } else {
                            false
                        }
                    }

                    InputEvent.Type.keyDown -> {
                        if (keyDown != null) {
                            keyDown(event, event.keyCode)
                        } else {
                            false
                        }
                    }

                    InputEvent.Type.keyTyped -> {
                        if (keyTyped != null) {
                            keyTyped(event, event.character)
                        } else {
                            false
                        }
                    }

                    else -> false
                }
            }
            false
        }
    }

    fun setFocus(actor: Actor?) {
        ui.stage.keyboardFocus = actor
    }

    fun getFocus(): Actor? = ui.stage.keyboardFocus

    fun addToRoot(actors: Iterable<Actor>) {
        for (actor in actors) {
            ui.bundlesRoot.add(actor)
        }
    }

    fun addToRoot(hud: HudApi) {
        addToRoot(hud.delegate.actors)
    }

    fun loadUI(
        xmlFilePath: String,
        i18nBundle: String,
        theme: ThemeApi?,
        actions: Map<String, (Any, Array<out Any>) -> Any?>
    ): CompletableFuture<HudApi> {
        val parser = SeleneLmlParser.parser().skin(theme?.skin ?: ui.systemSkin)

        for ((actionName, actionFunction) in actions) {
            parser.action(actionName, object : ParameterizedActorConsumer<Any?, Any> {
                override fun consumeWithParameters(widget: Any, vararg parameters: Any): Any? {
                    return actionFunction(widget, parameters)
                }
            })
        }

        val i18nFileHandle = bundleFileResolver.resolve(i18nBundle)
        val bundle = I18NBundle.createBundle(i18nFileHandle)
        parser.i18nBundle(bundle)

        val xmlFile = bundleFileResolver.resolve(xmlFilePath)
        if (!xmlFile.exists()) {
            throw IllegalArgumentException("XML file not found: $xmlFilePath")
        }

        val actors = parser.build().parseTemplate(xmlFile)

        val actorsByName = mutableMapOf<String, Actor>()
        fun collectActorsByName(actor: Actor) {
            if (actor.name != null && actor.name.isNotEmpty()) {
                actorsByName[actor.name] = actor
            }
            if (actor is Group) {
                actor.children.forEach { child ->
                    collectActorsByName(child)
                }
            }
        }
        actors.forEach { collectActorsByName(it) }
        val hud = Hud(actors.toList(), actorsByName).api
        return CompletableFuture.completedFuture(hud);
    }

    fun loadTheme(skinPath: String, atlas: TextureAtlas?): CompletableFuture<ThemeApi> {
        val skinFile = bundleFileResolver.resolve(skinPath)
        if (!skinFile.exists()) {
            throw IllegalArgumentException("Skin file not found: $skinPath")
        }
        val theme = ThemeApi(atlas?.let { Skin(skinFile, it) } ?: Skin(skinFile), skinResolvers, assetProvider)
        return CompletableFuture.completedFuture(theme)
    }

    fun loadTheme(themeDefinition: ThemeDefinition, atlas: TextureAtlas?): CompletableFuture<ThemeApi> {
        val skin = atlas?.let { Skin(it) } ?: Skin()
        val font = BitmapFont()
        skin.add("default", font)
        skin.add("default", Label.LabelStyle(font, Color.WHITE))
        skin.add("hidden", ImageButton.ImageButtonStyle())
        themeDefinition.applyToSkin(skin, skinResolvers)
        val theme = ThemeApi(skin, skinResolvers, assetProvider)
        return CompletableFuture.completedFuture(theme)
    }

    fun createTheme(): ThemeApi {
        val font = BitmapFont()
        return ThemeApi(Skin().apply {
            add("default", font)
            add("default", Label.LabelStyle(font, Color.WHITE))
            add("hidden", ImageButton.ImageButtonStyle())
        }, skinResolvers, assetProvider)
    }

    fun createContainer(
        theme: ThemeApi,
        child: Actor?,
        background: String?,
        width: Float?,
        height: Float?
    ): Container<Actor> {
        val container = Container<Actor>(child)
        background?.let {
            container.background = skinResolvers.resolveDrawable(theme, it)
        }
        width?.let {
            container.width(it)
        }
        height?.let {
            container.height(it)
        }
        return container
    }

    fun createLabel(theme: ThemeApi, style: String, text: String, wrap: Boolean): Label {
        val labelStyle = theme.skin.get(style, Label.LabelStyle::class.java)
        val label = Label(text, labelStyle)
        label.wrap = wrap
        return label
    }

    fun createDragListener(
        onStart: ((Draggable, Actor, Float, Float) -> Boolean)?,
        onDrag: ((Draggable, Actor, Float, Float) -> Unit)?,
        onEnd: ((Draggable, Actor, Float, Float) -> Boolean)?
    ): Draggable.DragListener {
        return object : Draggable.DragListener {
            override fun onStart(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float): Boolean {
                if (onStart != null) {
                    return onStart(draggable, actor, stageX, stageY)
                }
                return true
            }

            override fun onDrag(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float) {
                if (onDrag != null) {
                    onDrag(draggable, actor, stageX, stageY)
                }
            }

            override fun onEnd(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float): Boolean {
                if (onEnd != null) {
                    return onEnd(draggable, actor, stageX, stageY)
                }
                return true
            }
        }
    }

    fun createAtlas(textures: Map<String, Any?>): CompletableFuture<TextureAtlas> {
        val atlas = TextureAtlas()
        textures.entries.forEach { (name, path) ->
            val textureFile = bundleFileResolver.resolve(path.toString())
            atlas.addRegion(name, TextureRegion(Texture(textureFile)))
        }
        return CompletableFuture.completedFuture(atlas)
    }
}
