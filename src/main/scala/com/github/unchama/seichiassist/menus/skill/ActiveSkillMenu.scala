package com.github.unchama.seichiassist.menus.skill

import cats.effect.IO
import com.github.unchama.generic.CachedFunction
import com.github.unchama.menuinventory.slot.button.Button
import com.github.unchama.menuinventory.{ChestSlotRef, Menu, MenuFrame, MenuSlotLayout}
import com.github.unchama.seichiassist.activeskill.SeichiSkill
import com.github.unchama.seichiassist.menus.CommonButtons
import org.bukkit.ChatColor.{BOLD, DARK_PURPLE}
import org.bukkit.entity.Player

object ActiveSkillMenu extends Menu {
  import com.github.unchama.menuinventory.syntax._

  override val frame: MenuFrame = MenuFrame(5.chestRows, s"$DARK_PURPLE${BOLD}整地スキル選択")

  private case class ButtonComputations(player: Player) {

    import com.github.unchama.seichiassist.concurrent.PluginExecutionContexts.{layoutPreparationContext, syncShift}
    import player._

    import scala.util.chaining._

    val computeStatusButton: IO[Button] = {
      ???
    }

    val normalSeichiSkillButton: CachedFunction[(Boolean, Boolean, SeichiSkill), Button] =
      CachedFunction { case (unlocked, selected, skill) =>
        ???
      }

    def computeNormalSkillButtonFor(skill: SeichiSkill): IO[Button] = {
      ???
    }

    val assaultArmorButton: CachedFunction[(Boolean, Boolean), Button] =
      CachedFunction { case (unlocked, selected) =>
        ???
      }

    val venderBlizzardButton: CachedFunction[(Boolean, Boolean), Button] =
      CachedFunction { case (unlocked, selected) =>
        ???
      }

    val computeAssaultArmorButton: IO[Button] = {
      ???
    }

    val computeVenderBlizzardButton: IO[Button] = {
      ???
    }
  }

  private object ConstantButtons {
    val skillEffectMenuButton: Button = {
      ???
    }

    val resetSkillsButton: Button = {
      ???
    }
  }

  override def computeMenuLayout(player: Player): IO[MenuSlotLayout] = {
    import cats.implicits._
    import eu.timepit.refined.auto._

    val buttonComputations = ButtonComputations(player)
    import buttonComputations._
    import ConstantButtons._

    val constantPart = Map(
      ChestSlotRef(0, 1) -> resetSkillsButton,
      ChestSlotRef(0, 2) -> skillEffectMenuButton,
      ChestSlotRef(4, 0) -> CommonButtons.openStickMenu
    )

    import SeichiSkill._

    val dynamicPartComputation = List(
      ChestSlotRef(0, 0) -> computeStatusButton,

      ChestSlotRef(0, 3) -> computeNormalSkillButtonFor(EbifriDrive),
      ChestSlotRef(0, 4) -> computeNormalSkillButtonFor(HolyShot),
      ChestSlotRef(0, 5) -> computeNormalSkillButtonFor(TsarBomba),
      ChestSlotRef(0, 6) -> computeNormalSkillButtonFor(ArcBlast),
      ChestSlotRef(0, 7) -> computeNormalSkillButtonFor(PhantasmRay),
      ChestSlotRef(0, 8) -> computeNormalSkillButtonFor(Supernova),

      ChestSlotRef(1, 3) -> computeNormalSkillButtonFor(TomBoy),
      ChestSlotRef(1, 4) -> computeNormalSkillButtonFor(Thunderstorm),
      ChestSlotRef(1, 5) -> computeNormalSkillButtonFor(StarlightBreaker),
      ChestSlotRef(1, 6) -> computeNormalSkillButtonFor(EarthDivide),
      ChestSlotRef(1, 7) -> computeNormalSkillButtonFor(HeavenGaeBolg),
      ChestSlotRef(1, 8) -> computeNormalSkillButtonFor(Decision),

      ChestSlotRef(2, 0) -> computeNormalSkillButtonFor(DualBreak),
      ChestSlotRef(2, 1) -> computeNormalSkillButtonFor(TrialBreak),
      ChestSlotRef(2, 2) -> computeNormalSkillButtonFor(Explosion),
      ChestSlotRef(2, 3) -> computeNormalSkillButtonFor(MirageFlare),
      ChestSlotRef(2, 4) -> computeNormalSkillButtonFor(Dockarn),
      ChestSlotRef(2, 5) -> computeNormalSkillButtonFor(GiganticBomb),
      ChestSlotRef(2, 6) -> computeNormalSkillButtonFor(BrilliantDetonation),
      ChestSlotRef(2, 7) -> computeNormalSkillButtonFor(LemuriaImpact),
      ChestSlotRef(2, 8) -> computeNormalSkillButtonFor(EternalVice),

      ChestSlotRef(3, 3) -> computeNormalSkillButtonFor(WhiteBreath),
      ChestSlotRef(3, 4) -> computeNormalSkillButtonFor(AbsoluteZero),
      ChestSlotRef(3, 5) -> computeNormalSkillButtonFor(DiamondDust),

      ChestSlotRef(4, 3) -> computeNormalSkillButtonFor(LavaCondensation),
      ChestSlotRef(4, 4) -> computeNormalSkillButtonFor(MoerakiBoulders),
      ChestSlotRef(4, 5) -> computeNormalSkillButtonFor(Eldfell),

      ChestSlotRef(1, 1) -> computeAssaultArmorButton,
      ChestSlotRef(3, 1) -> computeVenderBlizzardButton,
    )
      .map(_.sequence)
      .sequence

    for {
      dynamicPart <- dynamicPartComputation
    } yield MenuSlotLayout(constantPart ++ dynamicPart)
  }
}
