package io.github.kabos.pomodoro.timer

import android.content.Context
import androidx.wear.tiles.TileService
import io.github.kabos.pomodoro.PomodoroTileService
import io.github.kabos.pomodoro.complication.PomodoroComplications
import io.github.kabos.pomodoro.history.PomodoroHistory
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import java.time.ZoneId

/**
 * 状態の更新・アラーム予約・振動・記録をまとめる入口。
 * UI と BroadcastReceiver はここだけを呼ぶ。
 */
class TimerController(context: Context) {
  private val appContext = context.applicationContext
  private val store = TimerStore.get(appContext)
  private val scheduler = AlarmScheduler(appContext)
  private val haptics = Haptics(appContext)
  private val log = EventLog(appContext)
  private val notifier = TimerNotifier(appContext)
  private val history = PomodoroHistory.get(appContext)

  val state: StateFlow<TimerState> get() = store.state

  /** 最初の 1 回だけ手動で開始する。同じ日の再開なら「何ポモ目か」は引き継ぐ。 */
  fun start(now: Long = System.currentTimeMillis()) {
    val today = epochDay(now)
    val carried = PomodoroCycle.todayCount(store.state.value, store.countDay, today)
    store.countDay = today

    val next = PomodoroCycle.start(now, store.durations, carried)
    store.update(next)
    scheduler.schedule(next.phaseEndAt)
    publish(next, now)
    log.append("START", next, now)
  }

  /** 会議・外出などで手動停止。残り時間は捨てる。 */
  fun stop(now: Long = System.currentTimeMillis()) {
    scheduler.cancel()
    PomodoroCycle.finishedWork(store.state.value, now, store.durations, completed = false)
      ?.let(history::record)
    val next = PomodoroCycle.stop().copy(completedWorkCount = store.state.value.completedWorkCount)
    store.update(next)
    publish(next, now)
    log.append("STOP", now = now)
  }

  /** アラームが鳴った。終わったフェーズに合わせて振動し、次のフェーズを即座に始める。 */
  fun onPhaseEnded(now: Long = System.currentTimeMillis()) {
    val previous = store.state.value
    if (!previous.isActive) {
      log.append("ALARM_IGNORED(idle)", now = now)
      return
    }
    PomodoroCycle.finishedWork(previous, now, store.durations, completed = true)?.let(history::record)

    // 動いたまま 0 時を越えたら、今日の数は 0 から数え直す
    val today = epochDay(now)
    val base = PomodoroCycle.rollOverDay(previous, store.countDay, today)
    store.countDay = today

    val next = PomodoroCycle.advance(base, now, store.durations)
    store.update(next)
    scheduler.schedule(next.phaseEndAt)
    publish(next, now)
    when (previous.phase) {
      Phase.WORK -> haptics.workEnded()
      Phase.MEDITATION -> haptics.meditationEnded()
      Phase.IDLE -> Unit
    }
    log.append("PHASE_END ${previous.phase}->${next.phase}", next, now)
  }

  /**
   * 再起動・アプリ更新・画面復帰のときに呼ぶ。
   * 予約が消えていても状態から張り直す。終了時刻を過ぎていればその場で次へ進める。
   */
  fun ensureScheduled(reason: String, now: Long = System.currentTimeMillis()) {
    val current = store.state.value
    if (!current.isActive) {
      publish(current, now)
      return
    }
    if (current.phaseEndAt <= now) {
      log.append("CATCH_UP($reason)", current, now)
      onPhaseEnded(now)
    } else {
      scheduler.schedule(current.phaseEndAt)
      publish(current, now)
      log.append("RESCHEDULE($reason)", current, now)
    }
  }

  fun setDurations(durations: Durations) {
    store.durations = durations
    log.append("DURATIONS work=${durations.workMs / 1000}s meditation=${durations.meditationMs / 1000}s")
  }

  fun readLog(): String = log.readAll()

  /** 今日終えた作業の数。止めたまま日をまたいだら 0。 */
  fun todayCount(now: Long = System.currentTimeMillis()): Int =
    PomodoroCycle.todayCount(store.state.value, store.countDay, epochDay(now))

  /** 通知欄・文字盤（Ongoing Activity とコンプリケーション）・タイルに今の状態を反映する。 */
  private fun publish(state: TimerState, now: Long) {
    notifier.show(state, PomodoroCycle.todayCount(state, store.countDay, epochDay(now)), now)
    TileService.getUpdater(appContext).requestUpdate(PomodoroTileService::class.java)
    PomodoroComplications.requestUpdateAll(appContext)
  }

  private fun epochDay(now: Long): Long =
    Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
}
