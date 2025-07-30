package world.selene.server.data

import world.selene.common.data.ComponentRegistry
import world.selene.common.data.EntityRegistry
import world.selene.common.data.NameIdRegistry
import world.selene.common.data.TileRegistry
import world.selene.common.data.TransitionRegistry

class Registries(
    val mappings: NameIdRegistry,
    val tiles: TileRegistry,
    val transitions: TransitionRegistry,
    val entities: EntityRegistry,
    val components: ComponentRegistry
)