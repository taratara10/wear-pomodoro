package io.github.kabos.pomodoro.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.CountDownTimeReference
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingTimelineComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.TimeInterval
import androidx.wear.watchface.complications.datasource.TimelineEntry
import io.github.kabos.pomodoro.complication.PomodoroComplications.plain
import io.github.kabos.pomodoro.timer.Phase
import io.github.kabos.pomodoro.timer.TimerStore
import io.github.kabos.pomodoro.timer.label
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * いまのフェーズの残り時間を分単位で出すコンプリケーション。
 *
 * 残り時間は終了時刻へのカウントダウン（[TimeDifferenceComplicationText]）として渡し、
 * 文字盤側が描くたびに数える。最小単位を分にしているので、表示が変わるのは 1 分に 1 回。
 * アプリが更新を送るのはフェーズの切り替わり（アラーム）と開始・停止のときだけ。
 */
class RemainingTimeComplicationService : SuspendingTimelineComplicationDataSourceService() {

  override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationDataTimeline? {
    val type = request.complicationType
    val state = TimerStore.get(this).state.value
    val idle = data(type, Phase.IDLE, plain("--")) ?: return null
    if (!state.isActive) return ComplicationDataTimeline(idle, emptyList())

    val now = Instant.now()
    val end = Instant.ofEpochMilli(state.phaseEndAt)
    // 終了時刻を過ぎてからアラームの更新が届くまでは、数字を出さずに待つ
    val waiting = data(type, state.phase, plain("--"))!!
    if (!end.isAfter(now)) return ComplicationDataTimeline(waiting, emptyList())
    return ComplicationDataTimeline(
      defaultComplicationData = waiting,
      timelineEntries = listOf(
        TimelineEntry(TimeInterval(now, end), data(type, state.phase, countdown(end))!!),
      ),
    )
  }

  override fun getPreviewData(type: ComplicationType): ComplicationData? =
    data(type, Phase.WORK, plain("18分"))

  private fun countdown(end: Instant): ComplicationText =
    TimeDifferenceComplicationText.Builder(
      TimeDifferenceStyle.SHORT_SINGLE_UNIT,
      CountDownTimeReference(end),
    )
      .setMinimumTimeUnit(TimeUnit.MINUTES)
      .build()

  private fun data(type: ComplicationType, phase: Phase, remaining: ComplicationText): ComplicationData? {
    val title = plain(phase.label())
    return when (type) {
      ComplicationType.SHORT_TEXT ->
        ShortTextComplicationData.Builder(remaining, title)
          .setTitle(title)
          .setMonochromaticImage(PomodoroComplications.icon(this))
          .setTapAction(PomodoroComplications.openApp(this))
          .build()
      ComplicationType.LONG_TEXT ->
        LongTextComplicationData.Builder(remaining, title)
          .setTitle(title)
          .setMonochromaticImage(PomodoroComplications.icon(this))
          .setTapAction(PomodoroComplications.openApp(this))
          .build()
      else -> null
    }
  }
}
