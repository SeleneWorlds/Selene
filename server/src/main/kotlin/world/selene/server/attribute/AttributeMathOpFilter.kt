package world.selene.server.attribute

sealed class AttributeMathOpFilter<T>(val value: MathOpValue<Int>, val op: MathOp) :
    AttributeFilter<T> {
    enum class MathOp(val sign: String) {
        ADD("+"),
        SUBTRACT("-"),
        MULTIPLY("*"),
        DIVIDE("/");

        companion object {
            private val BY_SIGN = entries.associateBy { it.sign }

            fun fromSign(sign: String): MathOp {
                return BY_SIGN[sign] ?: throw IllegalArgumentException("Invalid math op sign: $sign")
            }
        }
    }

    interface MathOpValue<T> {
        val value: T
    }

    data class ConstantMathOpValue<T>(override val value: T) : MathOpValue<T>
    data class AttributeMathOpValue<T>(val attribute: Attribute<T>) : MathOpValue<T> {
        override val value: T get() = attribute.effectiveValue
    }

    override var enabled: Boolean = true
}

class IntAttributeMathOpFilter(value: MathOpValue<Int>, op: MathOp) :
    AttributeMathOpFilter<Int>(value, op) {
    override fun apply(attribute: Attribute<Int>, value: Int): Int {
        return when (op) {
            MathOp.ADD -> value + this.value.value
            MathOp.SUBTRACT -> value - this.value.value
            MathOp.MULTIPLY -> value * this.value.value
            MathOp.DIVIDE -> value / this.value.value
        }
    }
}