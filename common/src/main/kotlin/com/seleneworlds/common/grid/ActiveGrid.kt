package com.seleneworlds.common.grid

import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.data.RegistryReloadListener

class ActiveGrid(
    private val grid: Grid,
    private val gridRegistry: GridRegistry
) : RegistryReloadListener<GridDefinition> {

    private var preferredGridId: Identifier? = null
    var activeGridId: Identifier? = null
        private set

    init {
        gridRegistry.addReloadListener(this)
    }

    fun applyDefaultGrid(): Identifier {
        preferredGridId = null
        return reapply()
    }

    fun applyGrid(identifier: Identifier?): Identifier {
        preferredGridId = identifier
        return reapply()
    }

    fun applyGrid(identifier: String?): Identifier {
        return applyGrid(identifier?.takeIf { it.isNotBlank() }?.let(Identifier::parse))
    }

    fun reapply(): Identifier {
        val selectedGridId = preferredGridId ?: gridRegistry.getAll().keys.firstOrNull()
            ?: throw IllegalStateException("No grids have been registered")
        val definition = gridRegistry.get(selectedGridId)
            ?: throw IllegalStateException("Unknown active grid: $selectedGridId")
        grid.applyDefinition(definition)
        activeGridId = selectedGridId
        return selectedGridId
    }

    override fun onRegistryReloaded(registry: Registry<GridDefinition>) {
        if (registry.getAll().isNotEmpty()) {
            reapply()
        }
    }
}
