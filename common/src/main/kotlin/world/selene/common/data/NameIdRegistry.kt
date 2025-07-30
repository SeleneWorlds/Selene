package world.selene.common.data

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table

open class NameIdRegistry {
    val mappings: Table<String, String, Int> = HashBasedTable.create()
    protected val reverseMappings: Table<String, Int, String> = HashBasedTable.create()

    fun getId(scope: String, name: String): Int? = mappings[scope, name]

    fun getName(scope: String, id: Int): String? = reverseMappings[scope, id]

    fun clear(scope: String) {
        mappings.rowKeySet().remove(scope)
        reverseMappings.rowKeySet().remove(scope)
    }

    fun clearAll() {
        mappings.clear()
        reverseMappings.clear()
    }

    fun addExisting(scope: String, name: String, id: Int) {
        mappings.put(scope, name, id)
        reverseMappings.put(scope, id, name)
    }
}