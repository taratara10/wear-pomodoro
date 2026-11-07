package io.github.kabos.pomodoro.timer

/** サイクルのフェーズ。IDLE は停止中。 */
enum class Phase { IDLE, WORK, MEDITATION }

/** 作業と瞑想の長さ。試作では 25分 / 5分。debug ビルドでは adb から変えられる。 */
data class Durations(val workMs: Long, val meditationMs: Long) {
  companion object {
    val DEFAULT = Durations(workMs = 25 * 60 * 1000L, meditationMs = 5 * 60 * 1000L)
  }
}

/**
 * 永続化される最小の状態。
 * 残り時間は持たず、[phaseEndAt]（壁時計の epoch ミリ秒）から毎回計算する。
 * プロセスが死んでもアラームとこの状態だけで続きが再現できる。
 */
data class TimerState(
  val phase: Phase = Phase.IDLE,
  /** 現在のフェーズが終わる時刻。IDLE のときは 0。 */
  val phaseEndAt: Long = 0L,
  /** 開始以降に終えた作業（25分）の数。何ポモ目かの表示用。 */
  val completedWorkCount: Int = 0,
  /** 現在のフェーズが始まった時刻。履歴に作業の開始時刻を残すため。IDLE のときは 0。 */
  val phaseStartAt: Long = 0L,
) {
  val isActive: Boolean get() = phase != Phase.IDLE

  fun remainingMs(now: Long): Long = if (isActive) (phaseEndAt - now).coerceAtLeast(0L) else 0L
}

/**
 * 25分作業 / 5分瞑想を自動で繰り返す純粋な状態遷移。
 * Android に依存しないので、ここだけ単体テストで固める。
 */
object PomodoroCycle {

  /** 手動開始。停止後の再開も残り時間は引き継がず、新しい作業から始める。 */
  fun start(now: Long, durations: Durations, carriedCount: Int = 0): TimerState =
    TimerState(
      phase = Phase.WORK,
      phaseEndAt = now + durations.workMs,
      completedWorkCount = carriedCount,
      phaseStartAt = now,
    )

  fun stop(): TimerState = TimerState()

  /**
   * 今日の時点で終えた作業の数。最後に数えた日 [countDay] が今日でなければ 0。
   * タイルや通知は、止めたまま日をまたいでも昨日の数を出さないようにこれを使う。
   */
  fun todayCount(state: TimerState, countDay: Long, today: Long): Int =
    if (countDay == today) state.completedWorkCount else 0

  /** 日をまたいでいたら数を 0 に戻した状態を返す。動いたまま 0 時を越えたときに使う。 */
  fun rollOverDay(state: TimerState, countDay: Long, today: Long): TimerState =
    if (countDay == today) state else state.copy(completedWorkCount = 0)

  /**
   * フェーズ終了のアラームが鳴った時点で呼ぶ。終わったフェーズに応じて次へ進める。
   * 次のフェーズは「アラームが鳴った今」から数える。遅れて鳴っても瞑想は丸ごと取れる。
   */
  fun advance(state: TimerState, now: Long, durations: Durations): TimerState =
    when (state.phase) {
      Phase.IDLE -> state
      Phase.WORK -> state.copy(
        phase = Phase.MEDITATION,
        phaseEndAt = now + durations.meditationMs,
        completedWorkCount = state.completedWorkCount + 1,
        phaseStartAt = now,
      )
      Phase.MEDITATION -> state.copy(
        phase = Phase.WORK,
        phaseEndAt = now + durations.workMs,
        phaseStartAt = now,
      )
    }

  /**
   * 終わった作業を履歴に残す形にする。作業中でなければ null。
   * - 最後まで終えた作業: 終了は予定の終了時刻（再起動で遅れて気づいても、作業は予定どおり終わっている）
   * - 途中で止めた作業: 終了は止めた時刻。押し間違いのような [MIN_RECORD_MS] 未満は残さない
   * 開始時刻を持たない古い状態からは、終了時刻と作業の長さから開始を逆算する。
   */
  fun finishedWork(state: TimerState, now: Long, durations: Durations, completed: Boolean): WorkSpan? {
    if (state.phase != Phase.WORK) return null
    val startAt = if (state.phaseStartAt > 0) state.phaseStartAt else state.phaseEndAt - durations.workMs
    val endAt = if (completed) state.phaseEndAt else minOf(now, state.phaseEndAt)
    if (!completed && endAt - startAt < MIN_RECORD_MS) return null
    return WorkSpan(startAt, endAt, completed)
  }

  const val MIN_RECORD_MS = 60_000L
}

/** 履歴に残す 1 回分の作業。[completed] が false なら途中で止めた。 */
data class WorkSpan(val startAt: Long, val endAt: Long, val completed: Boolean)

