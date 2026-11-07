package io.github.kabos.pomodoro.history

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 1 回分の作業（ポモ）の記録。時刻は壁時計の epoch ミリ秒。
 * 日付への振り分けは読むときに端末のタイムゾーンで行う。
 */
@Entity(tableName = "pomodoro", indices = [Index("startAt")])
data class PomodoroRecord(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val startAt: Long,
  val endAt: Long,
  /** 最後まで終えたら true。途中で止めたら false（回数には数えない）。 */
  val completed: Boolean,
)
