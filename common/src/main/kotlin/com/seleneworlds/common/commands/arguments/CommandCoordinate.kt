@file:Suppress("RedundantSuppression")

package com.seleneworlds.common.commands.arguments

import arrow.core.Either
import com.mojang.brigadier.StringReader
import com.seleneworlds.common.commands.CommandSource
import com.seleneworlds.common.commands.remains
import com.seleneworlds.common.commands.skipIf

class CommandCoordinate(@Suppress("unused") val relative: Boolean, val value: Int) {

    fun getValue(@Suppress("unused") source: CommandSource): Int {
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