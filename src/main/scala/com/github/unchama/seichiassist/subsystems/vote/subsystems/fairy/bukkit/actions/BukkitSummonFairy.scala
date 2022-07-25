package com.github.unchama.seichiassist.subsystems.vote.subsystems.fairy.bukkit.actions

import cats.effect.{ConcurrentEffect, ContextShift, IO, LiftIO, SyncIO}
import com.github.unchama.seichiassist.concurrent.PluginExecutionContexts.onMainThread
import com.github.unchama.seichiassist.subsystems.breakcount.BreakCountAPI
import com.github.unchama.seichiassist.subsystems.mana.ManaApi
import com.github.unchama.seichiassist.subsystems.vote.VoteAPI
import com.github.unchama.seichiassist.subsystems.vote.domain.EffectPoint
import com.github.unchama.seichiassist.subsystems.vote.subsystems.fairy.FairyAPI
import com.github.unchama.seichiassist.subsystems.vote.subsystems.fairy.application.actions.SummonFairy
import com.github.unchama.seichiassist.subsystems.vote.subsystems.fairy.bukkit.routines.FairySpeechRoutine
import com.github.unchama.seichiassist.subsystems.vote.subsystems.fairy.domain.FairyUsingState.Using
import com.github.unchama.seichiassist.subsystems.vote.subsystems.fairy.domain.{
  FairyRecoveryManaAmount,
  FairyUsingState
}
import com.github.unchama.targetedeffect.commandsender.MessageEffect
import com.github.unchama.targetedeffect.player.FocusedSoundEffect
import com.github.unchama.targetedeffect.{SequentialEffect, UnfocusedEffect}
import org.bukkit.ChatColor._
import org.bukkit.Sound
import org.bukkit.entity.Player

object BukkitSummonFairy {

  def apply(player: Player)(
    implicit breakCountAPI: BreakCountAPI[IO, SyncIO, Player],
    fairyAPI: FairyAPI[SyncIO],
    concurrentEffect: ConcurrentEffect[SyncIO],
    contextShift: ContextShift[IO],
    voteAPI: VoteAPI[SyncIO],
    manaApi: ManaApi[IO, SyncIO, Player]
  ): SummonFairy[IO] = new SummonFairy[IO] {
    override def summon: IO[Unit] = {
      val playerLevel =
        breakCountAPI
          .seichiAmountDataRepository(player)
          .read
          .unsafeRunSync()
          .levelCorrespondingToExp
          .level

      val notEnoughLevelEffect =
        SequentialEffect(
          MessageEffect(s"${GOLD}プレイヤーレベルが足りません"),
          FocusedSoundEffect(Sound.BLOCK_GLASS_PLACE, 1f, 0.1f)
        )
      val alreadySummoned =
        SequentialEffect(
          MessageEffect(s"${GOLD}既に妖精を召喚しています"),
          FocusedSoundEffect(Sound.BLOCK_GLASS_PLACE, 1f, 0.1f)
        )
      val notEnoughEffectPoint =
        SequentialEffect(
          MessageEffect(s"${GOLD}投票ptが足りません"),
          FocusedSoundEffect(Sound.BLOCK_GLASS_PLACE, 1f, 0.1f)
        )

      val uuid = player.getUniqueId

      val validTimeState = fairyAPI.fairyValidTimeState(uuid).toIO.unsafeRunSync()

      if (playerLevel < 10) return notEnoughLevelEffect(player) // レベル不足

      if (fairyAPI.fairyUsingState(uuid).toIO.unsafeRunSync() == Using)
        return alreadySummoned(player) // 既に召喚している

      if (voteAPI.effectPoints(uuid).toIO.unsafeRunSync().value < validTimeState.value * 2)
        return notEnoughEffectPoint(player) // 投票ptがたりなかった

      val levelCappedManaAmount =
        manaApi.readManaAmount(player).unsafeRunSync().cap.value

      // 回復するマナの量
      val recoveryMana = FairyRecoveryManaAmount.manaAmountAt(levelCappedManaAmount)

      val eff = for {
        _ <- fairyAPI.updateFairyUsingState(uuid, FairyUsingState.Using)
        _ <- voteAPI.decreaseEffectPoint(uuid, EffectPoint(validTimeState.value * 2))
        _ <- fairyAPI.updateFairyRecoveryManaAmount(uuid, recoveryMana)
        validTimes <- fairyAPI.fairyValidTimes(uuid)
        _ <- fairyAPI.updateFairyValidTimes(uuid, Some(validTimeState.validTime))
      } yield {
        /*
          FairySpeechRoutineが一度も起動されていなければ起動する
          そうじゃなかったからfor実行
         */
        validTimes match {
          case Some(_) => ()
          case None =>
            import com.github.unchama.seichiassist.concurrent.PluginExecutionContexts.sleepAndRoutineContext
            FairySpeechRoutine.start(player).start
            ()
        }
      }

      LiftIO[IO].liftIO {
        SequentialEffect(
          UnfocusedEffect(eff.toIO.unsafeRunAsyncAndForget()),
          MessageEffect(
            List(
              s"$RESET$YELLOW${BOLD}妖精を呼び出しました！",
              s"$RESET$YELLOW${BOLD}この子は1分間に約${recoveryMana.recoveryMana}マナ",
              s"$RESET$YELLOW${BOLD}回復させる力を持っているようです。"
            )
          )
        ).apply(player)
      }

    }
  }

}
