package io.github.kabos.pomodoro.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingTimelineComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.TimeInterval
import androidx.wear.watchface.complications.datasource.TimelineEntry
import io.github.kabos.pomodoro.complication.PomodoroComplications.plain
import io.github.kabos.pomodoro.timer.PomodoroCycle
import io.github.kabos.pomodoro.timer.TimerStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 今日終えた作業の数を出すコンプリケーション。
 *
 * 数が変わるのはフェーズの切り替わりだけなので、そのときに更新を頼まれるのを待つ。
 * 止めたまま 0 時を越えたときに昨日の数を出さないよう、「今日の数は 0 時まで、それ以降は 0」を
 * タイムラインで渡しておく。0 時にアプリを起こさずに済む。
 */
class TodayCountComplicationService : SuspendingTimelineComplicationDataSourceService() {

  override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationDataTimeline? {
    val type = request.complicationType
    val zero = data(type, 0) ?: return null

    val store = TimerStore.get(this)
    val zone = ZoneId.systemDefault()
    val now = Instant.now()
    val today = LocalDate.ofInstant(now, zone)
    val count = PomodoroCycle.todayCount(store.state.value, store.countDay, today.toEpochDay())
    if (count == 0) return ComplicationDataTimeline(zero, emptyList())

    val nextMidnight = today.plusDays(1).atStartOfDay(zone).toInstant()
    return ComplicationDataTimeline(
      defaultComplicationData = zero,
      timelineEntries = listOf(TimelineEntry(TimeInterval(now, nextMidnight), data(type, count)!!)),
    )
  }

  override fun getPreviewData(type: ComplicationType): ComplicationData? = data(type, 3)

  private fun data(type: ComplicationType, count: Int): ComplicationData? {
    val description = plain("今日 $count ポモ")
    return when (type) {
      ComplicationType.SHORT_TEXT ->
        ShortTextComplicationData.Builder(plain(count.toString()), description)
          .setTitle(plain("ポモ"))
          .setMonochromaticImage(PomodoroComplications.icon(this))
          .setTapAction(PomodoroComplications.openApp(this))
          .build()
      ComplicationType.LONG_TEXT ->
        LongTextComplicationData.Builder(plain("今日 $count ポモ"), description)
          .setMonochromaticImage(PomodoroComplications.icon(this))
          .setTapAction(PomodoroComplications.openApp(this))
          .build()
      else -> null
    }
  }
}
