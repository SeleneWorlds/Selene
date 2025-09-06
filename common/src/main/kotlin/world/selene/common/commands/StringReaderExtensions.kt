package world.selene.common.commands

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType

fun StringReader.remains(): Boolean {
    return canRead() && peek() != ' '
}

fun StringReader.skipIf(char: Char): Boolean {
    if (peek() == char) {
        skip()
        return true
    }
    return false
}

fun StringReader.expectOr(char: Char, supplier: () -> SimpleCommandExceptionType): Either<Throwable, Unit> {
    if (canRead() && peek() == char) {
        skip()
        return Unit.right()
    } else {
        return supplier.invoke().createWithContext(this).left()
    }
}

fun StringReader.expectOrThrow(char: Char, supplier: () -> SimpleCommandExceptionType) {
    if (!canRead() || peek() != char) {
        throw supplier().createWithContext(this)
    }
    skip()
}