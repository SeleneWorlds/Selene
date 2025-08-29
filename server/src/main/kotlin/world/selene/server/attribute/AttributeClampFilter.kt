package world.selene.server.attribute

sealed class AttributeClampFilter<T>(val min: ClampValue<T>, val max: ClampValue<T>) : AttributeFilter<T> {
    interface ClampValue<T> { val value: T }
    data class ConstantClampValue<T>(override val value: T) : ClampValue<T>
    data class AttributeClampValue<T>(val attribute: Attribute<T>) : ClampValue<T> {
        override val value: T get() = attribute.effectiveValue
    }
    override var enabled: Boolean = true
}

class IntAttributeClampFilter(min: ClampValue<Int>, max: ClampValue<Int>) : AttributeClampFilter<Int>(min, max) {
    override fun apply(attribute: Attribute<Int>, value: Int): Int {
        return value.coerceIn(min.value, max.value)
    }
}