package com.github.unchama.seichiassist.subsystems.gridregion.domain

trait RegionOperations[F[_], Location, Player] {

  /**
   * @return `currentLocation`から`regionUnits`を使って保護範囲の始点と終点を求める
   */
  def getSelection(
    currentLocation: Location,
    regionUnits: RegionUnits,
    direction: Direction
  ): RegionSelection[Location]

  /**
   * @return 保護を作成する作用
   */
  def createRegion(player: Player): F[Unit]

}
