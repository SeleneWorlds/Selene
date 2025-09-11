package world.selene.docgen

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.github.czyzby.lml.parser.impl.tag.Dtd
import com.google.common.collect.ArrayListMultimap
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
        val commonAttributesByType: Map<TagType, List<String>>
    )

    data class TagInfo(val tag: String, val attributes: List<String>)

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

        val tagsByType = mutableMapOf<TagType, MutableList<String>>()
        allTags.forEach { tag ->
            val tagType = determineTagType(tag, tagsToAttributes.get(tag))
            tagsByType.getOrPut(tagType) { mutableListOf() }.add(tag)
        }

        val commonAttributesByType = mutableMapOf<TagType, List<String>>()
        TagType.entries.forEach { tagType ->
            val tagsOfType = tagsByType[tagType] ?: emptyList()
            val commonAttributes = if (tagsOfType.isNotEmpty()) {
                tagsOfType
                    .map { tag -> tagsToAttributes.get(tag).toSet() }
                    .reduce { acc, tagAttributes -> acc.intersect(tagAttributes) }
                    .sorted()
            } else {
                emptyList()
            }
            commonAttributesByType[tagType] = commonAttributes
        }

        val tagInfoByType = mutableMapOf<TagType, List<TagInfo>>()
        TagType.entries.forEach { tagType ->
            val commonAttrsForType = commonAttributesByType[tagType] ?: emptyList()
            val tagsOfType = tagsByType[tagType] ?: emptyList()
            val tagInfoList = tagsOfType.sorted().map { tag ->
                val tagAttributes = tagsToAttributes.get(tag)
                    .filter { it !in commonAttrsForType }
                    .sorted()
                TagInfo(tag, tagAttributes)
            }
            tagInfoByType[tagType] = tagInfoList
        }

        return LmlInfo(tagInfoByType, commonAttributesByType)
    }

}