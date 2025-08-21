package world.selene.client.visual

data class TextureOptions(val flipX: Boolean, val flipY: Boolean) {
    companion object {
        val Default = TextureOptions(flipX = false, flipY = false)
    }
}