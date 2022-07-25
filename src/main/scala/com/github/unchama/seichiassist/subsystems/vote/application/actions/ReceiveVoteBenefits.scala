package com.github.unchama.seichiassist.subsystems.vote.application.actions

import com.github.unchama.seichiassist.subsystems.breakcount.BreakCountAPI
import com.github.unchama.seichiassist.subsystems.vote.VoteAPI

trait ReceiveVoteBenefits[F[_], G[_], Player] {

  def receive(
    player: Player
  )(implicit voteAPI: VoteAPI[G], breakCountAPI: BreakCountAPI[F, G, Player]): G[Unit]

}

object ReceiveVoteBenefits {

  def apply[F[_], G[_], Player](
    implicit ev: ReceiveVoteBenefits[F, G, Player]
  ): ReceiveVoteBenefits[F, G, Player] = ev

}
