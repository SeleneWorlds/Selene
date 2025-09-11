package world.selene.common.commands.arguments

import arrow.core.Option
import arrow.core.getOrElse
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import world.selene.common.commands.CommandSource
import world.selene.common.commands.suggestions.SuggestionSource
import world.selene.common.grid.Coordinate
import java.util.concurrent.CompletableFuture

class CoordinateArgument : ArgumentType<Coordinates> {

    override fun parse(reader: StringReader): Coordinates {
        return Coordinates.parseCoordinates(reader).getOrElse { throw it }
    }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        return Option.fromNullable(context.source as? SuggestionSource)
            .map {
                builder.suggest("0")
                builder.buildFuture()
            }
            .getOrElse { Suggestions.empty() }
    }

    companion object {
        fun coordinate(): CoordinateArgument {
            return CoordinateArgument()
        }

        fun getCoordinate(context: CommandContext<CommandSource>, name: String): Coordinate {
            return context.getArgument(name, Coordinates::class.java).getCoordinate(context.source)
        }
    }
}