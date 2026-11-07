package io.github.kabos.pomodoro.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroDao {
  @Insert
  suspend fun insert(record: PomodoroRecord)

  /** [fromMillis] 以降に始まった記録を新しい順に流す。 */
  @Query("SELECT * FROM pomodoro WHERE startAt >= :fromMillis ORDER BY startAt DESC")
  fun observeSince(fromMillis: Long): Flow<List<PomodoroRecord>>

  /** [fromMillis] 以上 [toMillis] 未満に始まった記録を古い順に流す。 */
  @Query("SELECT * FROM pomodoro WHERE startAt >= :fromMillis AND startAt < :toMillis ORDER BY startAt")
  fun observeBetween(fromMillis: Long, toMillis: Long): Flow<List<PomodoroRecord>>
}
