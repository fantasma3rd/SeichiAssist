package com.github.unchama.seichiassist.subsystems.gridregion

import cats.data.Kleisli
import cats.effect.SyncEffect
import cats.effect.concurrent.Ref
import com.github.unchama.datarepository.KeyedDataRepository
import com.github.unchama.datarepository.bukkit.player.BukkitRepositoryControls
import com.github.unchama.datarepository.template.RepositoryDefinition
import com.github.unchama.generic.ContextCoercion
import com.github.unchama.minecraft.bukkit.algebra.BukkitPlayerHasUuid.instance
import com.github.unchama.seichiassist.SeichiAssist
import com.github.unchama.seichiassist.meta.subsystem.Subsystem
import com.github.unchama.seichiassist.subsystems.gridregion.application.repository.{
  RegionCountRepositoryDefinition,
  RegionTemplateRepositoryDefinition,
  RegionUnitPerClickSettingRepositoryDefinition,
  RegionUnitsRepositoryDefinition
}
import com.github.unchama.seichiassist.subsystems.gridregion.bukkit.BukkitRegionOperations
import com.github.unchama.seichiassist.subsystems.gridregion.domain._
import com.github.unchama.seichiassist.subsystems.gridregion.domain.persistence.{
  RegionCountPersistence,
  RegionTemplatePersistence
}
import com.github.unchama.seichiassist.subsystems.gridregion.domain.regiontemplate.RegionTemplate
import com.github.unchama.seichiassist.subsystems.gridregion.infrastructure.{
  JdbcRegionCountPersistence,
  JdbcRegionTemplatePersistence
}
import org.bukkit.Location
import org.bukkit.entity.Player

trait System[F[_], Player, Location] extends Subsystem[F] {

  val api: GridRegionAPI[F, Player, Location]

}

object System {

  import cats.implicits._

  def wired[F[_], G[_]: SyncEffect: ContextCoercion[*[_], F]]
    : G[System[F, Player, Location]] = {
    implicit val regionCountPersistence: RegionCountPersistence[G] =
      new JdbcRegionCountPersistence[G]
    implicit val regionTemplatePersistence: RegionTemplatePersistence[G] =
      new JdbcRegionTemplatePersistence[G]

    for {
      regionUnitPerClickSettingRepositoryControls <- BukkitRepositoryControls.createHandles(
        RepositoryDefinition
          .Phased
          .TwoPhased(
            RegionUnitPerClickSettingRepositoryDefinition.initialization[G, Player],
            RegionUnitPerClickSettingRepositoryDefinition.finalization[G, Player]
          )
      )
      regionUnitsRepositoryControls <- BukkitRepositoryControls.createHandles(
        RepositoryDefinition
          .Phased
          .TwoPhased(
            RegionUnitsRepositoryDefinition.initialization[G, Player],
            RegionUnitsRepositoryDefinition.finalization[G, Player]
          )
      )
      regionCountRepositoryControls <- BukkitRepositoryControls.createHandles(
        RegionCountRepositoryDefinition.withContext[G, Player]
      )
      regionTemplateRepositoryControls <- BukkitRepositoryControls.createHandles(
        RegionTemplateRepositoryDefinition.withContext[G, Player]
      )
    } yield {
      val regionUnitPerClickSettingRepository =
        regionUnitPerClickSettingRepositoryControls.repository
      val regionUnitsRepository =
        regionUnitsRepositoryControls.repository
      implicit val regionCountRepository: KeyedDataRepository[Player, Ref[G, RegionCount]] =
        regionCountRepositoryControls.repository
      val regionOperations: RegionOperations[G, Location, Player] = new BukkitRegionOperations

      new System[F, Player, Location] {
        override val api: GridRegionAPI[F, Player, Location] =
          new GridRegionAPI[F, Player, Location] {
            override def toggleUnitPerClick: Kleisli[F, Player, Unit] = Kleisli { player =>
              ContextCoercion(regionUnitPerClickSettingRepository(player).toggleUnitPerClick)
            }

            override def unitPerClick(player: Player): F[RegionUnit] =
              ContextCoercion(regionUnitPerClickSettingRepository(player).unitPerClick)

            override def isWithinLimits(
              regionUnits: RegionUnits,
              worldName: String
            ): Boolean = {
              val totalRegionUnits =
                regionUnits.computeTotalRegionUnits
              val limit = SeichiAssist.seichiAssistConfig.getGridLimitPerWorld(worldName)

              totalRegionUnits.units <= limit
            }

            override def regionUnits(player: Player): F[RegionUnits] =
              ContextCoercion(regionUnitsRepository(player).regionUnits)

            override def saveRegionUnits(regionUnits: RegionUnits): Kleisli[F, Player, Unit] =
              Kleisli { player =>
                ContextCoercion(regionUnitsRepository(player).set(regionUnits))
              }

            override def regionUnitLimit(worldName: String): RegionUnitLimit = {
              val limit = SeichiAssist.seichiAssistConfig.getGridLimitPerWorld(worldName)
              RegionUnitLimit(limit)
            }

            override def canCreateRegion(
              player: Player,
              regionUnits: RegionUnits,
              direction: CardinalDirection
            ): F[CreateRegionResult] =
              ContextCoercion(regionOperations.canCreateRegion(player, regionUnits, direction))

            override def regionSelection(
              player: Player,
              regionUnits: RegionUnits,
              direction: CardinalDirection
            ): RegionSelection[Location] =
              regionOperations.getSelection(player.getLocation, regionUnits, direction)

            override def createRegion: Kleisli[F, Player, Unit] = Kleisli { player =>
              ContextCoercion(regionOperations.tryCreateRegion(player))
            }

            override def regionCount(player: Player): F[RegionCount] =
              ContextCoercion(regionCountRepository(player).get)

            override def savedGridRegionTemplate(player: Player): F[Vector[RegionTemplate]] =
              ContextCoercion(regionTemplateRepositoryControls.repository(player).get)

            override def saveGridRegionTemplate(
              regionTemplate: RegionTemplate
            ): Kleisli[F, Player, Unit] = Kleisli { player =>
              ContextCoercion(
                regionTemplateRepositoryControls.repository(player).update { oldTemplates =>
                  val templateListExcludingSameTemplateId =
                    oldTemplates.filterNot(_.templateId == regionTemplate.templateId)

                  regionTemplate +: templateListExcludingSameTemplateId
                }
              )
            }
          }

        override val managedRepositoryControls: Seq[BukkitRepositoryControls[F, _]] = Seq(
          regionUnitPerClickSettingRepositoryControls,
          regionUnitsRepositoryControls,
          regionCountRepositoryControls,
          regionTemplateRepositoryControls
        ).map(_.coerceFinalizationContextTo[F])
      }
    }
  }

}
