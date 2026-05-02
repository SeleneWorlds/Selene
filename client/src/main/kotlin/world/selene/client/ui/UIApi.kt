package world.selene.client.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.utils.I18NBundle
import com.kotcrab.vis.ui.widget.Draggable
import world.selene.client.bundles.BundleFileResolver
import world.selene.client.ui.lml.SeleneLmlParser
import kotlin.collections.iterator

/**
 * Load, skin and manipulate UIs.
 */
class UIApi(
    private val ui: UI,
    private val bundleFileResolver: BundleFileResolver,
    val skinResolvers: SkinResolvers
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

    fun loadUI(
        xmlFilePath: String,
        i18nBundle: String,
        skin: Skin?,
        actions: Map<String, (Any, Array<out Any>) -> Any?>
    ): Pair<List<Actor>, Map<String, Actor>> {
        val parser = SeleneLmlParser.parser().skin(skin ?: ui.systemSkin)

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
        return actors.toList() to actorsByName
    }

    fun loadSkin(skinPath: String): Skin {
        val skinFile = bundleFileResolver.resolve(skinPath)
        if (!skinFile.exists()) {
            throw IllegalArgumentException("Skin file not found: $skinPath")
        }
        return Skin(skinFile)
    }

    fun createSkin(): Skin {
        val font = BitmapFont()
        return Skin().apply {
            add("default", font)
            add("default", Label.LabelStyle(font, Color.WHITE))
            add("hidden", ImageButton.ImageButtonStyle())
        }
    }

    fun createContainer(
        skin: Skin,
        child: Actor?,
        background: String?,
        width: Float?,
        height: Float?
    ): Container<Actor> {
        val container = Container<Actor>(child)
        background?.let {
            container.background = skinResolvers.resolveDrawable(skin, it)
        }
        width?.let {
            container.width(it)
        }
        height?.let {
            container.height(it)
        }
        return container
    }

    fun createLabel(skin: Skin, style: String, text: String, wrap: Boolean): Label {
        val labelStyle = skin.get(style, Label.LabelStyle::class.java)
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
}