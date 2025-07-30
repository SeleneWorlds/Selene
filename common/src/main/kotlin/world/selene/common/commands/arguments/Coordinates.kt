package world.selene.common.commands.arguments

import arrow.core.Either
import arrow.core.raise.either
import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import kotlinx.coroutines.runBlocking
import world.selene.common.commands.CommandSource
import world.selene.common.commands.expectOr
import world.selene.common.util.Coordinate

interface Coordinates {
    companion object {
        private val ERROR_UNKNOWN_FLAG = SimpleCommandExceptionType(LiteralMessage("Expected -c flag"))

        fun parseCoordinates(reader: StringReader): Either<Throwable, Coordinates> = runBlocking {
            either {
                val dash = reader.peek()
                if (dash == '-') {
                    reader.skip()
                    parseFlagCoordinates(reader).bind()
                } else {
                    WorldCoordinates.parseWorldCoordinates(reader).bind()
                }
            }
        }

        private fun parseFlagCoordinates(reader: StringReader): Either<Throwable, Coordinates> {
            return Either.catch {
                when (reader.readString()) {
                    "c" -> CursorCoordinates
                    else -> throw ERROR_UNKNOWN_FLAG.createWithContext(reader)
                }
            }
        }
    }

    fun getCoordinate(source: CommandSource): Coordinate = getCoordinate(source)
}

object CursorCoordinates : Coordinates {
    override fun getCoordinate(source: CommandSource): Coordinate {
        throw NotImplementedError()
    }
}

class WorldCoordinates(val x: CommandCoordinate, val y: CommandCoordinate, val z: CommandCoordinate) : Coordinates {
    override fun getCoordinate(source: CommandSource): Coordinate {
        return Coordinate(
            x.getValue(source),
            y.getValue(source),
            z.getValue(source)
        )
    }

    companion object {
        private val ERROR_INCOMPLETE = SimpleCommandExceptionType(LiteralMessage("Expected x y z coordinates"))

        fun parseWorldCoordinates(reader: StringReader): Either<Throwable, WorldCoordinates> = runBlocking {
            either {
                val x = CommandCoordinate.Companion.parseCoordinate(reader).bind()
                reader.expectOr(' ', Companion::ERROR_INCOMPLETE).bind()
                val y = CommandCoordinate.Companion.parseCoordinate(reader).bind()
                reader.expectOr(' ', Companion::ERROR_INCOMPLETE).bind()
                val z = CommandCoordinate.Companion.parseCoordinate(reader).bind()
                WorldCoordinates(x, y, z)
            }
        }
    }
}