package io.github.kabos.pomodoro.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import io.github.kabos.pomodoro.MainActivity
import io.github.kabos.pomodoro.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * サイクルが動いている間、通知欄と文字盤に「いまのフェーズと残り時間」を出す。
 * アプリが裏に回っても、文字盤のアイコンや通知から 1 タップで戻れるようにするため。
 *
 * 残り時間はシステムが数える（通知のクロノメーターと Ongoing Activity の TimerPart）。
 * アプリ側はフェーズが変わったときに出し直すだけで、プロセス内で数え続けない。
 * 振動は [Haptics] が鳴らすので、通知自体は音も振動も出さない。
 */
class TimerNotifier(private val context: Context) {
  private val manager = NotificationManagerCompat.from(context)
  private val timeFormatter = DateTimeFormatter.ofPattern("H:mm").withZone(ZoneId.systemDefault())

  fun show(state: TimerState, todayCount: Int, now: Long = System.currentTimeMillis()) {
    if (!state.isActive) {
      cancel()
      return
    }
    if (!manager.areNotificationsEnabled()) return
    ensureChannel()

    val label = state.phase.label()
    val openApp = PendingIntent.getActivity(
      context,
      REQUEST_OPEN,
      Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_pomodoro)
      .setContentTitle("$label · 今日 $todayCount ポモ")
      .setContentText("${timeFormatter.format(Instant.ofEpochMilli(state.phaseEndAt))} まで")
      .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setContentIntent(openApp)
      // 終了時刻へのカウントダウンをシステムに描かせる
      .setWhen(state.phaseEndAt)
      .setShowWhen(true)
      .setUsesChronometer(true)
      .setChronometerCountDown(true)

    // TimerPart は elapsedRealtime 基準なので、壁時計の終了時刻を変換する
    val endElapsed = SystemClock.elapsedRealtime() + (state.phaseEndAt - now)
    val status = Status.Builder()
      .addTemplate("#phase# #time#")
      .addPart("phase", Status.TextPart(label))
      .addPart("time", Status.TimerPart(endElapsed))
      .build()

    OngoingActivity.Builder(context, NOTIFICATION_ID, builder)
      .setStaticIcon(R.drawable.ic_pomodoro)
      .setTouchIntent(openApp)
      .setStatus(status)
      .build()
      .apply(context)

    @Suppress("MissingPermission") // areNotificationsEnabled で確認済み
    manager.notify(NOTIFICATION_ID, builder.build())
  }

  fun cancel() {
    manager.cancel(NOTIFICATION_ID)
  }

  private fun ensureChannel() {
    val channel = NotificationChannel(
      CHANNEL_ID,
      context.getString(R.string.channel_timer),
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      setShowBadge(false)
      enableVibration(false)
      setSound(null, null)
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  }

  private companion object {
    const val CHANNEL_ID = "timer"
    const val NOTIFICATION_ID = 1
    const val REQUEST_OPEN = 10
  }
}

fun Phase.label(): String = when (this) {
  Phase.IDLE -> "停止中"
  Phase.WORK -> "作業"
  Phase.MEDITATION -> "瞑想"
}
