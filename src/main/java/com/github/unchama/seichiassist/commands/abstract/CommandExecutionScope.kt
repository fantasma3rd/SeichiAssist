package com.github.unchama.seichiassist.commands.abstract

import arrow.core.None
import arrow.core.Option
import arrow.effects.IO
import com.github.unchama.seichiassist.commands.abstract.response.asResponseToSender

object CommandExecutionScope {
    fun returnNone(): IO<CommandResponse> = IO.just(None)
    fun returnMessage(message: String): IO<CommandResponse> = IO.just(Option.just(message.asResponseToSender()))
}
