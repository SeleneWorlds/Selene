package world.selene.client.visual

import world.selene.client.data.VisualRegistry
import world.selene.common.data.TileRegistry
import world.selene.client.maps.Tile
import ktx.assets.async.AssetStorage
import world.selene.client.data.AnimatedVisualDefinition
import world.selene.client.data.AnimatorVisualDefinition
import world.selene.client.data.LabelVisualDefinition
import world.selene.client.data.SimpleVisualDefinition
import world.selene.client.data.VariantsVisualDefinition
import world.selene.common.data.EntityRegistry

class VisualManager(
    private val assetStorage: AssetStorage,
    private val tileRegistry: TileRegistry,
    private val entityRegistry: EntityRegistry,
    private val visualRegistry: VisualRegistry
) {
    private val visualCache = mutableMapOf<String, VisualInstance>()

    fun updateShared(delta: Float) {
        visualCache.values.forEach {
            it.updateShared(delta)
        }
    }

    fun getVisualInstance(tile: Tile): VisualInstance? {
        val tileName = tile.tileName ?: return null
        val tileDef = tileRegistry.get(tileName) ?: return null
        return buildInstance(tileDef.visual)
    }

    fun buildInstance(visualName: String, properties: Map<String, String> = emptyMap()): VisualInstance? {
        val visualDef = visualRegistry.get(visualName) ?: return null
        if (visualDef.isShared) {
            visualCache[visualName]?.let {
                return it
            }
        }

        val instance = when (visualDef) {
            is SimpleVisualDefinition -> SimpleVisualInstance(visualDef, assetStorage)
            is VariantsVisualDefinition -> VariantsVisualInstance(visualDef, assetStorage)
            is AnimatedVisualDefinition -> AnimatedVisualInstance(visualDef, assetStorage)
            is AnimatorVisualDefinition -> {
                AnimatorVisualInstance(visualDef, assetStorage)
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
