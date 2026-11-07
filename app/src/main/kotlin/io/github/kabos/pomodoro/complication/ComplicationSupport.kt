package io.github.kabos.pomodoro.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import io.github.kabos.pomodoro.MainActivity
import io.github.kabos.pomodoro.R

/**
 * コンプリケーション 2 つ（今日のポモ数・残り時間）で共有する部品。
 *
 * どちらも定期更新はしない（UPDATE_PERIOD_SECONDS = 0）。
 * 状態が変わったときに [TimerController] が [requestUpdateAll] で更新を頼むだけで、
 * 残り時間は文字盤側が分単位で数え、日付の切り替わりはタイムラインで先に渡しておく。
 */
object PomodoroComplications {

  fun requestUpdateAll(context: Context) {
    listOf(TodayCountComplicationService::class.java, RemainingTimeComplicationService::class.java)
      .forEach { service ->
        ComplicationDataSourceUpdateRequester
          .create(context, ComponentName(context, service))
          .requestUpdateAll()
      }
  }

  internal fun openApp(context: Context): PendingIntent =
    PendingIntent.getActivity(
      context,
      REQUEST_OPEN,
      Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

  internal fun icon(context: Context): MonochromaticImage =
    MonochromaticImage.Builder(Icon.createWithResource(context, R.drawable.ic_pomodoro)).build()

  internal fun plain(text: CharSequence) = PlainComplicationText.Builder(text).build()

  private const val REQUEST_OPEN = 20
}
