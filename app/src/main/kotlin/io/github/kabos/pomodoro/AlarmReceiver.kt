package io.github.kabos.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.kabos.pomodoro.history.PomodoroHistory
import io.github.kabos.pomodoro.timer.TimerController

/** フェーズ終了のアラームを受けて、次のフェーズへ進める。 */
class AlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_PHASE_END) return
    // 履歴の書き込みが終わるまでプロセスを生かしておく
    val pending = goAsync()
    TimerController(context).onPhaseEnded()
    PomodoroHistory.get(context).afterPendingWrites { pending.finish() }
  }

  companion object {
    const val ACTION_PHASE_END = "io.github.kabos.pomodoro.PHASE_END"
  }
}
