package io.github.kabos.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.kabos.pomodoro.timer.Durations
import io.github.kabos.pomodoro.timer.TimerController

/**
 * debug ビルド専用。実機で短いサイクルを試すために、作業/瞑想の長さを秒で上書きする。
 *
 * adb shell am broadcast -a io.github.kabos.pomodoro.DEBUG_SET_DURATIONS \
 *   -n io.github.kabos.pomodoro/.DebugReceiver --ei work_sec 60 --ei meditation_sec 20
 *
 * 省略した側は 25分 / 5分 に戻る。
 */
class DebugReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_SET_DURATIONS) return
    val durations = Durations(
      workMs = intent.getIntExtra("work_sec", Durations.DEFAULT.workMs.toInt() / 1000) * 1000L,
      meditationMs = intent.getIntExtra("meditation_sec", Durations.DEFAULT.meditationMs.toInt() / 1000) * 1000L,
    )
    TimerController(context).setDurations(durations)
    Log.i("Pomodoro", "durations set: $durations")
  }

  companion object {
    const val ACTION_SET_DURATIONS = "io.github.kabos.pomodoro.DEBUG_SET_DURATIONS"
  }
}
