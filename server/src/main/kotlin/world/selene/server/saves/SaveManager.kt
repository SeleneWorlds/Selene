package world.selene.server.saves

import world.selene.server.maps.MapTree
import java.io.File

class SaveManager(private val mapTreeFormat: MapTreeFormat) {

    fun save(file: File, savable: Any?) {
        file.parentFile.mkdirs()
        (savable as? MapTree.MapTreeLuaProxy)?.let { mapTreeLuaProxy ->
            mapTreeFormat.saveFullyInline(file, mapTreeLuaProxy.delegate)
        }
    }

    fun load(file: File): MapTree? {
        if (file.extension == "selenemap") {
            return mapTreeFormat.load(file)
        }
        return null
    }

}