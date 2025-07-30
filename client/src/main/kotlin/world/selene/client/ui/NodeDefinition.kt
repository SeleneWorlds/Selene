package world.selene.client.ui

data class NodeDefinition(
    val type: String,
    val name: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val anchor: Node.Anchor,
    val children: List<NodeDefinition>,
    val attributes: Map<String, String>
)