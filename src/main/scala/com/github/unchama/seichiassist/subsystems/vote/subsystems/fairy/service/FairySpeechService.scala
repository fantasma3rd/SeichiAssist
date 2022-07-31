package com.github.unchama.seichiassist.subsystems.vote.subsystems.fairy.service

import cats.effect.Sync
import cats.implicits.catsSyntaxFlatMapOps
import com.github.unchama.seichiassist.subsystems.vote.subsystems.fairy.domain.FairySpeechGateway
import com.github.unchama.seichiassist.subsystems.vote.subsystems.fairy.domain.property.{
  FairyMessage,
  FairyPlaySound
}

class FairySpeechService[F[_]: Sync](gateway: FairySpeechGateway[F]) {

  def makeSpeech(fairyMessage: FairyMessage, fairyPlaySound: FairyPlaySound): F[Unit] = {
    if (fairyPlaySound == FairyPlaySound.On)
      gateway.sendMessage(fairyMessage) >> gateway.playSpeechSound
    else
      gateway.sendMessage(fairyMessage)
  }

}
