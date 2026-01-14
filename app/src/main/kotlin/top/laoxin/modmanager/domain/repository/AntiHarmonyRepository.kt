package top.laoxin.modmanager.domain.repository

import kotlinx.coroutines.flow.Flow
import top.laoxin.modmanager.domain.bean.AntiHarmonyBean
import top.laoxin.modmanager.domain.bean.GameInfoBean
import top.laoxin.modmanager.domain.model.Result
interface AntiHarmonyRepository {
  /** Gets the anti-harmony status for the current game. */
  // fun getAntiHarmony(): Flow<AntiHarmonyBean?>

  /** Adds a game to the anti-harmony database. */
  suspend fun addGameToAntiHarmony(game: AntiHarmonyBean)

  /**
   * Switches the anti-harmony feature for a given game. This method handles all the business logic,
   * permission checks, and file operations.
   *
   * @param gameInfo The game to switch.
   * @param enable True to enable, false to disable.
   * @return A result code indicating success or failure.
   */
  suspend fun switchAntiHarmony(gameInfo: GameInfoBean, enable: Boolean): Result<Unit>
  fun getAntiHarmony(gamePackageName: String): Flow<AntiHarmonyBean?>

}
