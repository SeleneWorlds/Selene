package world.selene.common.commands

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder

fun <TSource> literal(name: String): LiteralArgumentBuilder<TSource> {
    return LiteralArgumentBuilder.literal(name)
}

fun <TSource, TArgument> argument(
    name: String,
    type: ArgumentType<TArgument>
): RequiredArgumentBuilder<TSource, TArgument> {
    return RequiredArgumentBuilder.argument(name, type)
}