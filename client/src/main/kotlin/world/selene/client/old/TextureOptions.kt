package world.selene.client.old

data class TextureOptions(val flipX: Boolean, val flipY: Boolean) {
    companion object {
        val Default = TextureOptions(flipX = false, flipY = false)
    }
}