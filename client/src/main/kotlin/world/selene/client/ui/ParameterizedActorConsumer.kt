package world.selene.client.ui

import com.github.czyzby.lml.parser.action.ActorConsumer

interface ParameterizedActorConsumer<TReturn, TActor> : ActorConsumer<TReturn, TActor> {
    fun consumeWithParameters(actor: TActor, vararg parameters: Any): TReturn

    override fun consume(actor: TActor): TReturn {
        return consumeWithParameters(actor)
    }
}