package world.selene.client.animator

interface Animator {
    fun getAnimation(): String
    fun update(delta: Float)
}