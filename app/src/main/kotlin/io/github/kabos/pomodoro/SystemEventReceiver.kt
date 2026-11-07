package io.github.kabos.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.kabos.pomodoro.history.PomodoroHistory
import io.github.kabos.pomodoro.timer.TimerController

/**
 * 再起動やアプリ更新のあとは AlarmManager の予約が消える。
 * 保存してある状態からアラームを張り直す。
 */
class SystemEventReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val reason = when (intent.action) {
      Intent.ACTION_BOOT_COMPLETED -> "boot"
      Intent.ACTION_MY_PACKAGE_REPLACED -> "package_replaced"
      else -> return
    }
    // 止まっている間に終わった作業は追いつきの中で履歴に書くので、書き終わるまで待つ
    val pending = goAsync()
    TimerController(context).ensureScheduled(reason)
    PomodoroHistory.get(context).afterPendingWrites { pending.finish() }
  }
}
