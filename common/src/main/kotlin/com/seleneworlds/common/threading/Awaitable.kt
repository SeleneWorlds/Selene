package com.seleneworlds.common.threading

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

class Awaitable<T> : CompletableFuture<T>() {

    fun resolve(value: T): Boolean = complete(value)

    fun reject(throwable: Throwable): Boolean = completeExceptionally(throwable)

    companion object {
        fun <T> completed(value: T): Awaitable<T> = Awaitable<T>().apply {
            complete(value)
        }

        fun <T> failed(throwable: Throwable): Awaitable<T> = Awaitable<T>().apply {
            completeExceptionally(throwable)
        }

        fun <T> from(stage: CompletionStage<T>): Awaitable<T> = Awaitable<T>().apply {
            stage.whenComplete { result, throwable ->
                if (throwable != null) {
                    reject(throwable)
                } else {
                    resolve(result)
                }
            }
        }
    }
}
