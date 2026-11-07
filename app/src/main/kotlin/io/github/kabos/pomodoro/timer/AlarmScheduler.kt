package io.github.kabos.pomodoro.timer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.kabos.pomodoro.AlarmReceiver
import io.github.kabos.pomodoro.MainActivity

/**
 * 「次のフェーズが終わる時刻」を 1 つだけ AlarmManager に予約する。
 *
 * setAlarmClock を使う理由: Doze やアプリスタンバイの影響を受けず、時刻どおりに端末を起こす。
 * 以前のウォッチアプリが長時間で途切れたのは、プロセス内でカウントしていたためと見ている。
 * 同じ PendingIntent で再予約すると前の予約は置き換わるので、多重に鳴ることはない。
 */
class AlarmScheduler(private val context: Context) {
  private val alarmManager = context.getSystemService(AlarmManager::class.java)

  // USE_EXACT_ALARM（API 33+、タイマー/目覚まし用途）を宣言しているので正確アラームを使える。
  // lint は SCHEDULE_EXACT_ALARM しか見ないため、ここだけ警告を抑える。
  @SuppressLint("MissingPermission")
  fun schedule(triggerAtMillis: Long) {
    val operation = phaseEndOperation()
    if (!alarmManager.canScheduleExactAlarms()) {
      // 想定外だが、黙って止まるより不正確でも鳴らす方がよい
      Log.w(TAG, "exact alarms not allowed; falling back to inexact alarm")
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
      return
    }
    val showApp = PendingIntent.getActivity(
      context,
      REQUEST_SHOW,
      Intent(context, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
    alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showApp), operation)
  }

  fun cancel() {
    alarmManager.cancel(phaseEndOperation())
  }

  private fun phaseEndOperation(): PendingIntent =
    PendingIntent.getBroadcast(
      context,
      REQUEST_PHASE_END,
      Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_PHASE_END),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

  private companion object {
    const val TAG = "Pomodoro"
    const val REQUEST_SHOW = 0
    const val REQUEST_PHASE_END = 1
  }
}
