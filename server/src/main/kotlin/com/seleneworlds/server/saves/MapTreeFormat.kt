package com.seleneworlds.server.saves

import com.seleneworlds.server.maps.tree.MapTree
import java.io.File

interface MapTreeFormat {
    fun load(file: File): MapTree
    fun saveFullyInline(file: File, mapTree: MapTree)
}