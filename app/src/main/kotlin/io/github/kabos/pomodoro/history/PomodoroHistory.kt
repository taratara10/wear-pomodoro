package io.github.kabos.pomodoro.history

import android.content.Context
import io.github.kabos.pomodoro.timer.WorkSpan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * ポモの履歴の読み書き。プロセス単位のシングルトン。
 *
 * 書き込みは 1 本の直列キューに積む。BroadcastReceiver から書くときは
 * [afterPendingWrites] で書き終わりを待ってから goAsync を閉じる（プロセスが先に終わらないように）。
 */
class PomodoroHistory private constructor(context: Context) {
  private val dao = HistoryDatabase.create(context).pomodoroDao()
  private val writes = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

  fun record(span: WorkSpan) {
    writes.launch {
      dao.insert(PomodoroRecord(startAt = span.startAt, endAt = span.endAt, completed = span.completed))
    }
  }

  /** それまでに積んだ書き込みが終わってから [block] を呼ぶ。キューが直列なので後ろに並べるだけでよい。 */
  fun afterPendingWrites(block: () -> Unit) {
    writes.launch { block() }
  }

  /** [days] 日前の 0 時以降の記録（新しい順）。 */
  fun observeRecentDays(days: Long, zone: ZoneId = ZoneId.systemDefault()): Flow<List<PomodoroRecord>> {
    val from = LocalDate.now(zone).minusDays(days - 1).atStartOfDay(zone).toInstant().toEpochMilli()
    return dao.observeSince(from)
  }

  /** ある 1 日の記録（古い順）。 */
  fun observeDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Flow<List<PomodoroRecord>> {
    val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return dao.observeBetween(from, to)
  }

  companion object {
    @Volatile private var instance: PomodoroHistory? = null

    fun get(context: Context): PomodoroHistory =
      instance ?: synchronized(this) {
        instance ?: PomodoroHistory(context.applicationContext).also { instance = it }
      }
  }
}
