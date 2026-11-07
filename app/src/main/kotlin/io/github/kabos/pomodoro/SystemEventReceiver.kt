package io.github.kabos.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.kabos.pomodoro.timer.TimerController

/**
 * 再起動やアプリ更新のあとは AlarmManager の予約が消える。
 * 保存してある状態からアラームを張り直す。
 */
class SystemEventReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      Intent.ACTION_BOOT_COMPLETED -> TimerController(context).ensureScheduled("boot")
      Intent.ACTION_MY_PACKAGE_REPLACED -> TimerController(context).ensureScheduled("package_replaced")
    }
  }
}
