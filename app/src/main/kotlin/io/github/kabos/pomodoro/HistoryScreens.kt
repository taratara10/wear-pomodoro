package io.github.kabos.pomodoro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import io.github.kabos.pomodoro.history.DaySummary
import io.github.kabos.pomodoro.history.HistorySummary
import io.github.kabos.pomodoro.history.PomodoroHistory
import io.github.kabos.pomodoro.history.PomodoroRecord
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** 一覧に出す日数。週の集計にも足りるよう、2 週間より長めにとる。 */
private const val RECENT_DAYS = 60L

@Composable
fun HistoryRoute(history: PomodoroHistory, onOpenDay: (LocalDate) -> Unit) {
  val zone = remember { ZoneId.systemDefault() }
  val records by remember { history.observeRecentDays(RECENT_DAYS, zone) }
    .collectAsStateWithLifecycle(initialValue = emptyList())
  val today = LocalDate.now(zone)
  HistoryScreen(
    week = HistorySummary.weekCounts(records, today, zone),
    days = HistorySummary.byDay(records, zone),
    today = today,
    onOpenDay = onOpenDay,
  )
}

/**
 * 記録の入口。上に今週の回数（月〜日の棒）、下に記録のある日の一覧（新しい順）。
 * 日をタップすると、その日の開始〜終了時刻の一覧へ進む。
 */
@Composable
fun HistoryScreen(
  week: List<Pair<LocalDate, Int>>,
  days: List<DaySummary>,
  today: LocalDate,
  onOpenDay: (LocalDate) -> Unit,
) {
  val listState = rememberTransformingLazyColumnState()
  ScreenScaffold(scrollState = listState, timeText = { TimeText() }) { contentPadding ->
    TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
      item { ListHeader { Text("今週 ${week.sumOf { it.second }} ポモ") } }
      item { WeekBars(week = week, today = today) }
      if (days.isEmpty()) {
        item {
          Text(
            text = "まだ記録はありません",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
          )
        }
      }
      items(days, key = { it.date.toEpochDay() }) { day ->
        Button(
          onClick = { onOpenDay(day.date) },
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.filledTonalButtonColors(),
          label = { Text(day.date.label(today)) },
          secondaryLabel = { Text("${day.completedCount} ポモ") },
        )
      }
    }
  }
}

/** 月〜日の 7 本の棒。高さは週の最大に合わせる。今日の棒だけ強調する。 */
@Composable
private fun WeekBars(week: List<Pair<LocalDate, Int>>, today: LocalDate) {
  val max = week.maxOf { it.second }.coerceAtLeast(1)
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.Bottom,
  ) {
    week.forEach { (date, count) ->
      val isToday = date == today
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = if (count > 0) "$count" else "",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
          modifier = Modifier
            .width(10.dp)
            .height((4 + 36 * count / max).dp)
            .background(
              color = when {
                isToday -> MaterialTheme.colorScheme.primary
                count > 0 -> MaterialTheme.colorScheme.primaryDim
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
              },
              shape = RoundedCornerShape(3.dp),
            ),
        )
        Text(
          text = date.dayOfWeek.shortJa(),
          style = MaterialTheme.typography.labelSmall,
          color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun DayRoute(history: PomodoroHistory, date: LocalDate) {
  val zone = remember { ZoneId.systemDefault() }
  val records by remember(date) { history.observeDay(date, zone) }
    .collectAsStateWithLifecycle(initialValue = emptyList())
  DayScreen(date = date, records = records, today = LocalDate.now(zone), zone = zone)
}

/** 1 日分のポモの開始〜終了時刻。途中で止めた作業は「停止」として薄く出す。 */
@Composable
fun DayScreen(date: LocalDate, records: List<PomodoroRecord>, today: LocalDate, zone: ZoneId) {
  val listState = rememberTransformingLazyColumnState()
  val time = remember(zone) { DateTimeFormatter.ofPattern("H:mm").withZone(zone) }
  val completed = records.count { it.completed }
  ScreenScaffold(scrollState = listState, timeText = { TimeText() }) { contentPadding ->
    TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
      item { ListHeader { Text("${date.label(today)}  $completed ポモ") } }
      if (records.isEmpty()) {
        item {
          Text(
            text = "記録はありません",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
      items(records, key = { it.id }) { record ->
        val span = "${time.format(Instant.ofEpochMilli(record.startAt))} – ${time.format(Instant.ofEpochMilli(record.endAt))}"
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = span,
            style = MaterialTheme.typography.titleMedium,
            color = if (record.completed) {
              MaterialTheme.colorScheme.onSurface
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
          )
          if (!record.completed) {
            Text(
              text = "  停止",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

/** 「今日」「昨日」、それ以外は「11/5（水）」。 */
private fun LocalDate.label(today: LocalDate): String = when (this) {
  today -> "今日"
  today.minusDays(1) -> "昨日"
  else -> "$monthValue/$dayOfMonth（${dayOfWeek.shortJa()}）"
}

private fun DayOfWeek.shortJa(): String = getDisplayName(TextStyle.SHORT, Locale.JAPANESE)

@WearPreviewDevices
@Composable
private fun HistoryScreenPreview() {
  val today = LocalDate.of(2026, 11, 7)
  val zone = ZoneId.of("Asia/Tokyo")
  fun rec(day: LocalDate, h: Int, m: Int, completed: Boolean = true): PomodoroRecord {
    val start = day.atTime(h, m).atZone(zone).toInstant().toEpochMilli()
    return PomodoroRecord(startAt = start, endAt = start + 25 * 60_000L, completed = completed)
  }
  val records = listOf(
    rec(today, 13, 0), rec(today, 13, 30), rec(today, 14, 0, completed = false),
    rec(today.minusDays(1), 15, 0), rec(today.minusDays(3), 10, 0),
  )
  MaterialTheme {
    HistoryScreen(
      week = HistorySummary.weekCounts(records, today, zone),
      days = HistorySummary.byDay(records, zone),
      today = today,
      onOpenDay = {},
    )
  }
}

@WearPreviewDevices
@Composable
private fun DayScreenPreview() {
  val today = LocalDate.of(2026, 11, 7)
  val zone = ZoneId.of("Asia/Tokyo")
  val start = today.atTime(13, 0).atZone(zone).toInstant().toEpochMilli()
  MaterialTheme {
    DayScreen(
      date = today,
      records = listOf(
        PomodoroRecord(1, start, start + 25 * 60_000L, true),
        PomodoroRecord(2, start + 30 * 60_000L, start + 42 * 60_000L, false),
      ),
      today = today,
      zone = zone,
    )
  }
}
