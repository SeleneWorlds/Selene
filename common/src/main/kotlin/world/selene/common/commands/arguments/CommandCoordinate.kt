package world.selene.common.commands.arguments

import arrow.core.Either
import com.mojang.brigadier.StringReader
import world.selene.common.commands.CommandSource
import world.selene.common.commands.remains
import world.selene.common.commands.skipIf

class CommandCoordinate(val relative: Boolean, val value: Int) {

    fun getValue(source: CommandSource): Int {
        return value // TODO support relative coordinates using ~ prefix
    }

    companion object {
        fun parseCoordinate(reader: StringReader): Either<Throwable, CommandCoordinate> {
            return Either.catch {
                val relative = reader.skipIf('~')
                val value = if (reader.remains()) reader.readInt() else 0
                CommandCoordinate(relative, value)
            }
        }
    }
}