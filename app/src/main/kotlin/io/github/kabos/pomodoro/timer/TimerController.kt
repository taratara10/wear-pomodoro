package io.github.kabos.pomodoro.timer

import android.content.Context
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

  val state: StateFlow<TimerState> get() = store.state

  /** 最初の 1 回だけ手動で開始する。同じ日の再開なら「何ポモ目か」は引き継ぐ。 */
  fun start(now: Long = System.currentTimeMillis()) {
    val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
    val carried = if (store.countDay == today) store.state.value.completedWorkCount else 0
    store.countDay = today

    val next = PomodoroCycle.start(now, store.durations, carried)
    store.update(next)
    scheduler.schedule(next.phaseEndAt)
    log.append("START", next, now)
  }

  /** 会議・外出などで手動停止。残り時間は捨てる。 */
  fun stop(now: Long = System.currentTimeMillis()) {
    scheduler.cancel()
    store.update(PomodoroCycle.stop())
    log.append("STOP", now = now)
  }

  /** アラームが鳴った。終わったフェーズに合わせて振動し、次のフェーズを即座に始める。 */
  fun onPhaseEnded(now: Long = System.currentTimeMillis()) {
    val previous = store.state.value
    if (!previous.isActive) {
      log.append("ALARM_IGNORED(idle)", now = now)
      return
    }
    val next = PomodoroCycle.advance(previous, now, store.durations)
    store.update(next)
    scheduler.schedule(next.phaseEndAt)
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
    if (!current.isActive) return
    if (current.phaseEndAt <= now) {
      log.append("CATCH_UP($reason)", current, now)
      onPhaseEnded(now)
    } else {
      scheduler.schedule(current.phaseEndAt)
      log.append("RESCHEDULE($reason)", current, now)
    }
  }

  fun setDurations(durations: Durations) {
    store.durations = durations
    log.append("DURATIONS work=${durations.workMs / 1000}s meditation=${durations.meditationMs / 1000}s")
  }

  fun readLog(): String = log.readAll()
}
