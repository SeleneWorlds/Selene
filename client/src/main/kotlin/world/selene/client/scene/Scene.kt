package world.selene.client.scene

class Scene {
    private val renderables = mutableListOf<Renderable>()

    private var batchInProgress = false
    private val batchItems = mutableListOf<Renderable>()

    private var updateInProgress = false
    private val dirtiedRenderables = mutableListOf<Renderable>()

    fun beginBatch() {
        batchInProgress = true
    }

    fun endBatch() {
        batchInProgress = false
        updateAllSorting()
        batchItems.clear()
    }

    fun add(renderable: Renderable) {
        val insertionIndex = renderables.binarySearch { existing ->
            val sortLayerComparison = renderable.sortLayer.compareTo(existing.sortLayer)
            if (sortLayerComparison != 0) {
                sortLayerComparison
            } else {
                existing.localSortLayer.compareTo(renderable.localSortLayer)
            }
        }
        
        val actualIndex = if (insertionIndex < 0) -(insertionIndex + 1) else insertionIndex
        renderables.add(actualIndex, renderable)
    }

    fun updateAllSorting() {
        renderables.sortWith(
            Comparator.comparingInt { it: Renderable -> it.sortLayer }.reversed()
                .thenComparingInt { it: Renderable -> it.localSortLayer })
    }

    fun beginUpdate() {
        updateInProgress = true
    }

    fun endUpdate() {
        updateInProgress = false
        for (renderable in dirtiedRenderables) {
            updateSorting(renderable)
        }
        dirtiedRenderables.clear()
    }

    fun updateSorting(renderable: Renderable) {
        if (updateInProgress) {
            dirtiedRenderables.add(renderable)
        } else {
            updateAllSorting()
        }
    }

    fun remove(renderable: Renderable) {
        renderables.remove(renderable)
    }

    fun getOrderedRenderables(): List<Renderable> {
        return renderables
    }
}
