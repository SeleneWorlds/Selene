package world.selene.client.visual

import world.selene.client.data.VisualRegistry
import world.selene.client.maps.Tile
import world.selene.client.assets.AssetProvider
import world.selene.client.data.AnimatedVisualDefinition
import world.selene.client.data.AnimatorVisualDefinition
import world.selene.client.data.LabelVisualDefinition
import world.selene.client.data.SimpleVisualDefinition
import world.selene.client.data.VariantsVisualDefinition
import world.selene.common.lua.LuaManager

class VisualManager(
    private val assetProvider: AssetProvider,
    private val luaManager: LuaManager,
    private val visualRegistry: VisualRegistry
) {
    private val visualCache = mutableMapOf<String, VisualInstance>()

    fun updateShared(delta: Float) {
        visualCache.values.forEach {
            it.updateShared(delta)
        }
    }

    fun getVisualInstance(tile: Tile): VisualInstance? {
        return buildInstance(tile.tileDefinition.visual)
    }

    fun buildInstance(visualName: String, properties: Map<String, String> = emptyMap()): VisualInstance? {
        val visualDef = visualRegistry.get(visualName) ?: return null
        if (visualDef.isShared) {
            visualCache[visualName]?.let {
                return it
            }
        }

        val instance = when (visualDef) {
            is SimpleVisualDefinition -> SimpleVisualInstance(visualDef, assetProvider)
            is VariantsVisualDefinition -> VariantsVisualInstance(visualDef, assetProvider)
            is AnimatedVisualDefinition -> AnimatedVisualInstance(visualDef, assetProvider, luaManager)
            is AnimatorVisualDefinition -> {
                AnimatorVisualInstance(visualDef, assetProvider)
            }

            is LabelVisualDefinition -> LabelVisualInstance(visualDef, properties["label"] ?: visualDef.label)

            else -> null
        }?.also {
            if (visualDef.isShared) {
                visualCache[visualName] = it
            }
        }
        return instance
    }
}
