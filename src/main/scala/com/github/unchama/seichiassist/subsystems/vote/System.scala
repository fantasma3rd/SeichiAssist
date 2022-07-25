package com.github.unchama.seichiassist.subsystems.vote

import cats.effect.ConcurrentEffect
import com.github.unchama.minecraft.actions.OnMinecraftServerThread
import com.github.unchama.seichiassist.meta.subsystem.Subsystem
import com.github.unchama.seichiassist.subsystems.vote.bukkit.command.VoteCommand
import com.github.unchama.seichiassist.subsystems.vote.domain.{
  ChainVoteDayNumber,
  EffectPoint,
  PlayerName,
  VoteBenefit,
  VoteCounter
}
import com.github.unchama.seichiassist.subsystems.vote.infrastructure.JdbcVotePersistence
import org.bukkit.command.TabExecutor

import java.util.UUID

trait System[F[_]] extends Subsystem[F] {
  val api: VoteAPI[F]
}

object System {

  def wired[F[_]: ConcurrentEffect: OnMinecraftServerThread]: System[F] = {
    val votePersistence = new JdbcVotePersistence[F]

    new System[F] {
      override implicit val api: VoteAPI[F] = new VoteAPI[F] {
        override def voteCounterIncrement(playerName: PlayerName): F[Unit] =
          votePersistence.voteCounterIncrement(playerName)

        override def updateChainVote(playerName: PlayerName): F[Unit] =
          votePersistence.updateChainVote(playerName)

        override def voteCounter(uuid: UUID): F[VoteCounter] =
          votePersistence.voteCounter(uuid)

        override def chainVoteDayNumber(uuid: UUID): F[ChainVoteDayNumber] =
          votePersistence.chainVoteDays(uuid)

        override def decreaseEffectPoint(uuid: UUID, effectPoint: EffectPoint): F[Unit] =
          votePersistence.decreaseEffectPoints(uuid, effectPoint)

        override def increaseEffectPointsByTen(uuid: UUID): F[Unit] =
          votePersistence.increaseEffectPointsByTen(uuid)

        override def effectPoints(uuid: UUID): F[EffectPoint] =
          votePersistence.effectPoints(uuid)

        override def increaseVoteBenefits(uuid: UUID, benefit: VoteBenefit): F[Unit] =
          votePersistence.increaseVoteBenefits(uuid, benefit)

        override def receivedVoteBenefits(uuid: UUID): F[VoteBenefit] =
          votePersistence.receivedVoteBenefits(uuid)

        import cats.implicits._

        override def notReceivedVoteBenefits(uuid: UUID): F[VoteBenefit] = for {
          voteCounter <- voteCounter(uuid)
          receivedVote <- receivedVoteBenefits(uuid)
        } yield VoteBenefit(voteCounter.value - receivedVote.value)
      }

      override val commands: Map[String, TabExecutor] = Map(
        "vote" -> new VoteCommand[F].executor
      )
    }
  }

}
