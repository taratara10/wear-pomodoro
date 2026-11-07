package io.github.kabos.pomodoro

import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.TypeBuilders
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture
import io.github.kabos.pomodoro.timer.Phase
import io.github.kabos.pomodoro.timer.TimerState
import io.github.kabos.pomodoro.timer.TimerStore
import io.github.kabos.pomodoro.timer.label
import io.github.kabos.pomodoro.timer.PomodoroCycle
import java.time.Instant
import java.time.ZoneId

/**
 * 今日のポモドーロ状況を見るタイル。
 * いまのフェーズ・残り時間・今日終えた作業の数（ドット）を出し、タップでアプリを開く。
 *
 * 残り時間はタイルの描画側（プラットフォームの時刻）で数える。
 * アプリはフェーズが変わったときに [TileService.getUpdater] で更新を頼むだけ。
 */
class PomodoroTileService : TileService() {

  override fun onTileRequest(
    requestParams: RequestBuilders.TileRequest,
  ): ListenableFuture<TileBuilders.Tile> {
    val store = TimerStore.get(this)
    val now = System.currentTimeMillis()
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val state = store.state.value
    val count = PomodoroCycle.todayCount(state, store.countDay, today.toEpochDay())

    // 止めている間も、日付が変わったら数を 0 に戻すために翌日 0 時で古くする
    val nextMidnight = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val freshUntil = if (state.isActive) minOf(state.phaseEndAt, nextMidnight) else nextMidnight

    val tile = TileBuilders.Tile.Builder()
      .setResourcesVersion(RESOURCES_VERSION)
      .setFreshnessIntervalMillis((freshUntil - now).coerceAtLeast(MIN_FRESHNESS_MS))
      .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(layout(state, count, now)))
      .build()
    return CallbackToFutureAdapter.getFuture { it.set(tile) }
  }

  override fun onTileResourcesRequest(
    requestParams: RequestBuilders.ResourcesRequest,
  ): ListenableFuture<ResourceBuilders.Resources> {
    val resources = ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
    return CallbackToFutureAdapter.getFuture { it.set(resources) }
  }

  private fun layout(state: TimerState, count: Int, now: Long): LayoutElementBuilders.LayoutElement {
    val accent = state.phase.accent()
    val openApp = ModifiersBuilders.Clickable.Builder()
      .setId("open")
      .setOnClick(
        ActionBuilders.LaunchAction.Builder()
          .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
              .setPackageName(packageName)
              .setClassName(MainActivity::class.java.name)
              .build(),
          )
          .build(),
      )
      .build()

    val column = Column.Builder()
      .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
      .addContent(text(state.phase.label(), 16f, accent))
      .addContent(remainingText(state, now))
      .addContent(Spacer.Builder().setHeight(dp(6f)).build())
      .addContent(dots(count, accent))
      .addContent(Spacer.Builder().setHeight(dp(6f)).build())
      .addContent(text("今日 $count ポモ", 14f, COLOR_SECONDARY))
      .build()

    return Box.Builder()
      .setWidth(expand())
      .setHeight(expand())
      .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(openApp).build())
      .addContent(column)
      .build()
  }

  /** 動いている間は終了時刻までのカウントダウン。止めている間は "--:--"。 */
  private fun remainingText(state: TimerState, now: Long): LayoutElementBuilders.LayoutElement {
    if (!state.isActive) return text("--:--", 40f, COLOR_SECONDARY, bold = true)

    val remaining = DynamicInstant.platformTimeWithSecondsPrecision()
      .durationUntil(DynamicInstant.withSecondsPrecision(Instant.ofEpochMilli(state.phaseEndAt)))
    val twoDigits = DynamicInt32.IntFormatter.Builder().setMinIntegerDigits(2).build()
    val countdown = remaining.toIntMinutes().format(twoDigits)
      .concat(DynamicString.constant(":"))
      .concat(remaining.secondsPart.format(twoDigits))
    // 更新が届くまでの間にマイナスを出さない
    val clamped = DynamicString.onCondition(remaining.toIntSeconds().gt(0))
      .use(countdown)
      .elseUse(DynamicString.constant("00:00"))

    val staticValue = formatRemaining(state.remainingMs(now))
    return Text.Builder()
      .setText(TypeBuilders.StringProp.Builder(staticValue).setDynamicValue(clamped).build())
      .setLayoutConstraintsForDynamicText(TypeBuilders.StringLayoutConstraint.Builder("00:00").build())
      .setFontStyle(fontStyle(40f, COLOR_PRIMARY, bold = true))
      .build()
  }

  /** 今日終えた作業を 1 つずつドットで並べる。多い日は数字だけに任せる。 */
  private fun dots(count: Int, accent: Int): LayoutElementBuilders.LayoutElement {
    val row = Row.Builder().setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
    val shown = count.coerceAtMost(MAX_DOTS).coerceAtLeast(1)
    repeat(shown) { i ->
      if (i > 0) row.addContent(Spacer.Builder().setWidth(dp(4f)).build())
      val color = if (count == 0) COLOR_EMPTY_DOT else accent
      row.addContent(
        Box.Builder()
          .setWidth(dp(8f))
          .setHeight(dp(8f))
          .setModifiers(
            ModifiersBuilders.Modifiers.Builder()
              .setBackground(
                ModifiersBuilders.Background.Builder()
                  .setColor(argb(color))
                  .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(4f)).build())
                  .build(),
              )
              .build(),
          )
          .build(),
      )
    }
    return row.build()
  }

  private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
    Text.Builder().setText(value).setFontStyle(fontStyle(size, color, bold)).build()

  private fun fontStyle(size: Float, color: Int, bold: Boolean) =
    FontStyle.Builder()
      .setSize(sp(size))
      .setColor(argb(color))
      .setWeight(
        if (bold) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL,
      )
      .build()

  private fun Phase.accent(): Int = when (this) {
    Phase.WORK -> COLOR_WORK
    Phase.MEDITATION -> COLOR_MEDITATION
    Phase.IDLE -> COLOR_IDLE
  }

  private fun formatRemaining(ms: Long): String {
    val totalSeconds = (ms + 999) / 1_000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
  }

  private companion object {
    const val RESOURCES_VERSION = "1"
    const val MAX_DOTS = 12
    const val MIN_FRESHNESS_MS = 60_000L
    const val COLOR_PRIMARY = 0xFFFFFFFF.toInt()
    const val COLOR_SECONDARY = 0xFFBDBDBD.toInt()
    const val COLOR_WORK = 0xFFFF8A65.toInt()
    const val COLOR_MEDITATION = 0xFF80CBC4.toInt()
    const val COLOR_IDLE = 0xFF9E9E9E.toInt()
    const val COLOR_EMPTY_DOT = 0xFF424242.toInt()
  }
}
