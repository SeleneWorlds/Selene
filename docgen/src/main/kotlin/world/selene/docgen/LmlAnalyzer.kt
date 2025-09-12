package world.selene.docgen

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.github.czyzby.lml.parser.impl.tag.Dtd
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashMultiset
import world.selene.client.ui.lml.SeleneLmlParser
import java.io.File

class LmlAnalyzer {

    enum class TagType {
        ACTOR,
        MACRO,
        VALIDATOR,
        LISTENER,
        OTHER
    }

    fun writeDtd(appendable: Appendable) {
        Lwjgl3Application(object : ApplicationAdapter() {
            override fun create() {
                val parser = SeleneLmlParser.parser().build()
                Dtd.saveSchema(parser, appendable)
                Gdx.app.exit()
            }
        })
    }

    data class LmlInfo(
        val tagsByType: Map<TagType, List<TagInfo>>,
        val attributeSets: List<AttributeSet>
    )

    data class TagInfo(
        val tag: String,
        val attributes: List<String>,
        val attributeSets: List<String>
    )

    data class AttributeSet(
        val id: String,
        val attributes: List<String>
    ) {
        constructor(id: String, attributes: Set<String>)
                : this(id, attributes.map { it.lowercase() }.sorted().toList())
    }

    val knownAttributeSets = listOf(
        AttributeSet(
            "Actor", setOf(
                "action", "onShow",
                "alpha", "a",
                "blue", "b",
                "green", "g",
                "color",
                "red", "r",
                "debug",
                "id",
                "multiline",
                "onChange", "change",
                "onClick", "click",
                "onClose", "close", "onTagClose", "tagClose",
                "onCreate", "create", "onInit", "init",
                "rotation", "angle",
                "scale",
                "scaleX",
                "scaleY",
                "tooltip",
                "touchable",
                "node",
                "visible",
                "x",
                "y",
                "originX",
                "originY",
                "fillParent", // Layout only
                "layout", "layoutEnabled",
                "pack",
                "disabled", "disable", // Disableable only,
                "disableOnError", "disableOnFormError", "formDisable",
                "skin", // LmlActorBuilder
                "style", "class",
                "result", "onResult",
                "toButtonTable",
                "toDialogTable",
                "toTitleTable",
                "colorpicker", // Vis extensions
                "responsiveColorPicker",
                "visTooltip",
                "focusBorder",
                "focusBorderEnabled",
                "footer", "header", // ListView children
                "result", "onResult", "onDialogResult" // Dialog children
            )
        ),
        AttributeSet(
            "Group", setOf(
                "transform",
                "debugRecursively"
            )
        ),
        AttributeSet(
            "Listener", setOf(
                "if",
                "keep",
                "ids"
            )
        ),
        AttributeSet(
            "InputListener", setOf(
                "combined",
                "keys"
            )
        ),
        AttributeSet(
            "Cell", setOf(
                "align",
                "colspan",
                "expand",
                "expandX",
                "expandY",
                "fill",
                "fillX",
                "fillY",
                "grow",
                "growX",
                "growY",
                "height",
                "maxHeight",
                "maxSize",
                "maxWidth",
                "minHeight",
                "minSize",
                "minWidth",
                "padBottom",
                "padLeft",
                "pad",
                "padRight",
                "padTop",
                "prefHeight",
                "prefSize",
                "prefWidth",
                "size",
                "spaceBottom",
                "spaceLeft",
                "space",
                "spaceRight",
                "spaceTop",
                "uniform",
                "uniformX",
                "uniformY",
                "width",
                "row",
            )
        ),
        AttributeSet(
            "CellDefaults", setOf(
                "defaultalign",
                "defaultcolspan",
                "defaultexpand",
                "defaultexpandX",
                "defaultexpandY",
                "defaultfill",
                "defaultfillX",
                "defaultfillY",
                "defaultgrow",
                "defaultgrowX",
                "defaultgrowY",
                "defaultheight",
                "defaultmaxHeight",
                "defaultmaxSize",
                "defaultmaxWidth",
                "defaultminHeight",
                "defaultminSize",
                "defaultminWidth",
                "defaultpadBottom",
                "defaultpadLeft",
                "defaultpad",
                "defaultpadRight",
                "defaultpadTop",
                "defaultprefHeight",
                "defaultprefSize",
                "defaultprefWidth",
                "defaultsize",
                "defaultspaceBottom",
                "defaultspaceLeft",
                "defaultspace",
                "defaultspaceRight",
                "defaultspaceTop",
                "defaultuniform",
                "defaultuniformX",
                "defaultuniformY",
                "defaultwidth"
            )
        ),
        AttributeSet(
            "Table", setOf(
                "oneColumn",
                "tableAlign",
                "bg", "background",
                "tablePadBottom",
                "tablePadLeft",
                "tablePad",
                "tablePadRight",
                "tablePadTop",
                "round",
                "useCellDefaults", "useVisDefaults"
            )
        ),
        AttributeSet(
            "TextField", setOf(
                "blink", "blinkTime",
                "cursor", "cursorPosition",
                "digitsOnly", "numeric",
                "textAlign", "inputAlign", "textAlignment",
                "max", "maxLength",
                "message", "messageText",
                "passwordCharacter",
                "passwordMode", "password",
                "selectAll",
                "filter", "textFilter", "textFieldFilter",
                "listener", "textListener", "textFieldListener"
            )
        ),
        AttributeSet(
            "VisTextField", setOf(
                "cursorPos",
                "enterKeyFocusTraversal",
                "ignoreEqualsTextChange",
                "passCharacter",
                "readOnly"
            )
        ),
        AttributeSet(
            "Image", setOf(
                "imageAlign",
                "scaling", "imageScaling"
            )
        ),
        AttributeSet(
            "VisValidatableTextField", setOf(
                "restore", "restoreLastValid",
                "enabled", "validate", "validationEnabled"
            )
        ),
        AttributeSet(
            "Button", setOf(
                "checked",
                "programmaticChangeEvents",
                "requireChecked", "formChecked", "notCheckedError",
                "uncheckedError",
                "requireUnchecked", "requireNotChecked",
                "formUnchecked", "checkedError"
            )
        ),
        AttributeSet(
            "Label", setOf(
                "ellipsis",
                "labelAlign", "labelAlignment",
                "lineAlign", "lineAlignment",
                "textAlign", "textAlignment",
                "wrap",
                "errorMessage", "errorLabel", "errorMsgLabel",
                "errorMessageLabel",
                "text"
            )
        ),
        AttributeSet(
            "ScrollPane", setOf(
                "barsOnTop", "scrollbarsOnTop",
                "barsPositions", "scrollBarsPositions",
                "cancelTouchFocus",
                "clamp",
                "disable", "disabled", "scrollingDisabled",
                "disableX", "disabledX", "scrollingDisabledX",
                "disableY", "disabledY", "scrollingDisabledY",
                "fadeBars", "fadeScrollbars",
                "setupFadeScrollBars",
                "flick", "flickScroll",
                "flickScrollTapSquareSize", "tapSquareSize",
                "flingTime",
                "force", "forceScroll",
                "forceX", "forceScrollX",
                "forceY", "forceScrollY",
                "overscroll",
                "setupOverscroll",
                "overscrollX",
                "overscrollY",
                "scrollPercent", "percent",
                "scrollPercentX", "percentX",
                "scrollPercentY", "percentY",
                "variableSizeKnobs",
                "smooth", "smoothScrolling",
                "velocity",
                "velocityX",
                "velocityY",
            )
        )
    )

    private fun determineTagType(tag: String, attributes: List<String>): TagType {
        return when {
            tag.startsWith(":") -> TagType.MACRO
            tag.endsWith("validator") -> TagType.VALIDATOR
            tag.endsWith("listener") -> TagType.LISTENER
            attributes.contains("id") -> TagType.ACTOR
            else -> TagType.OTHER
        }
    }

    fun analyzeDtd(dtdFile: File): LmlInfo {
        val allTags = mutableSetOf<String>()
        val tagsToAttributes = ArrayListMultimap.create<String, String>()

        // Parse DTD file to extract tags and their attributes
        dtdFile.useLines { lines ->
            lines.forEach { line ->
                if (line.startsWith("<!ELEMENT ")) {
                    val parts = line.split(" ")
                    val tag = parts[1]
                    allTags.add(tag)
                } else if (line.startsWith("<!ATTLIST ")) {
                    val parts = line.split(" ")
                    val tag = parts[1]
                    val attribute = parts[2]
                    tagsToAttributes.put(tag, attribute)
                }
            }
        }

        // Group tags by type
        val tagsByType = mutableMapOf<TagType, MutableList<String>>()
        allTags.forEach { tag ->
            val tagType = determineTagType(tag, tagsToAttributes.get(tag))
            tagsByType.getOrPut(tagType) { mutableListOf() }.add(tag)
        }

        // Use the statically defined attribute sets
        val attributeSets = knownAttributeSets

        // Create tag info grouped by type with cluster references
        val tagInfoByType = mutableMapOf<TagType, List<TagInfo>>()
        TagType.entries.forEach { tagType ->
            val tagsOfType = tagsByType[tagType] ?: emptyList()
            val tagInfoList = tagsOfType.sorted().map { tag ->
                val tagAttributes = tagsToAttributes.get(tag).map { it.lowercase() }.toSet()

                // Find which clusters this tag references (contains all attributes of the cluster)
                val clusterReferences = attributeSets
                    .filter { cluster -> cluster.attributes.all { attr -> attr in tagAttributes } }
                    .map { it.id }

                // Remaining attributes are those not covered by any cluster
                val clusteredAttributes = attributeSets
                    .filter { it.id in clusterReferences }
                    .flatMap { it.attributes }
                    .toSet()

                val remainingAttributes = tagAttributes
                    .filter { it !in clusteredAttributes }
                    .sorted()

                TagInfo(
                    tag = tag,
                    attributes = remainingAttributes,
                    attributeSets = clusterReferences
                )
            }
            tagInfoByType[tagType] = tagInfoList
        }

        val attributeCounts = HashMultiset.create<String>()
        tagInfoByType.forEach {
            it.value.forEach { tagInfo ->
                attributeCounts.addAll(tagInfo.attributes)
            }
        }
        attributeCounts.entrySet().sortedBy { it.count }.forEach {
            println("${it.element}: ${it.count}") }

        return LmlInfo(tagInfoByType, attributeSets)
    }


}