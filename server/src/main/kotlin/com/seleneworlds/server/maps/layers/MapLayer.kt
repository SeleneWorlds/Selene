package com.seleneworlds.server.maps.layers

import com.seleneworlds.common.tiles.TileDefinition
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.serialization.SerializedMap

interface MapLayer {
    val name: String
    val visibilityTags: Set<String>
    val collisionTags: Set<String>
    fun placeTile(coordinate: Coordinate, tileDef: TileDefinition): Boolean
    fun replaceTiles(coordinate: Coordinate, tileDef: TileDefinition): Boolean
    fun swapTile(coordinate: Coordinate, tileDef: TileDefinition, newTileDef: TileDefinition): Boolean
    fun removeTile(coordinate: Coordinate, tileDef: TileDefinition): Boolean
    fun resetTile(coordinate: Coordinate)
    fun annotateTile(coordinate: Coordinate, key: String, data: SerializedMap?)
    fun addVisibilityTag(tagName: String)
    fun removeVisibilityTag(tagName: String)
    fun addCollisionTag(tagName: String)
    fun removeCollisionTag(tagName: String)
}
