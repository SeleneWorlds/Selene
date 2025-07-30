package world.selene.common.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaProxyProvider

data class EntityDefinition(val components: Map<String, ConfiguredComponent> = emptyMap())

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = VisualComponent::class, name = "visual"),
    JsonSubTypes.Type(value = ClientScriptComponent::class, name = "client_script")
)
interface ConfiguredComponent

data class VisualComponent(val visual: String, val properties: Map<String, String> = emptyMap()) :
    ConfiguredComponent, InstancedComponent<VisualComponent>,
    LuaProxyProvider<VisualComponent.VisualComponentLuaProxy> {

    @get:JsonIgnore
    override val luaProxy = VisualComponentLuaProxy(this)

    var r = 1f
    var g = 1f
    var b = 1f
    var a = 1f

    override fun instantiate(): VisualComponent {
        return copy()
    }

    class VisualComponentLuaProxy(private val delegate: VisualComponent) {
        fun SetColor(r: Float, g: Float, b: Float, a: Float) {
            delegate.r = r
            delegate.g = g
            delegate.b = b
            delegate.a = a
        }
    }
}

data class ClientScriptComponent(val script: String) : ConfiguredComponent,
    InstancedComponent<ClientScriptComponent> {
    @get:JsonIgnore
    var module: LuaValue? = null

    @get:JsonIgnore
    var data: LuaValue? = null

    override fun instantiate(): ClientScriptComponent {
        return copy()
    }
}