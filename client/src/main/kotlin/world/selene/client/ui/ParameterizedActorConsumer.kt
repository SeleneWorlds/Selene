package world.selene.client.ui

import com.github.czyzby.lml.parser.action.ActorConsumer

interface ParameterizedActorConsumer<TReturn, TWidget> : ActorConsumer<TReturn, TWidget> {
    fun consumeWithParameters(widget: TWidget, vararg parameters: Any): TReturn

    override fun consume(widget: TWidget): TReturn {
        return consumeWithParameters(widget)
    }
}