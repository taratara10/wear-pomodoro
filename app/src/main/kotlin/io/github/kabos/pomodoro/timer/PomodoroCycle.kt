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
    )

  fun stop(): TimerState = TimerState()

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
      )
      Phase.MEDITATION -> state.copy(
        phase = Phase.WORK,
        phaseEndAt = now + durations.workMs,
      )
    }
}
