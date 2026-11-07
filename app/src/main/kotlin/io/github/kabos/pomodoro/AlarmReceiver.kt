package io.github.kabos.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.kabos.pomodoro.timer.TimerController

/** フェーズ終了のアラームを受けて、次のフェーズへ進める。 */
class AlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_PHASE_END) return
    TimerController(context).onPhaseEnded()
  }

  companion object {
    const val ACTION_PHASE_END = "io.github.kabos.pomodoro.PHASE_END"
  }
}
