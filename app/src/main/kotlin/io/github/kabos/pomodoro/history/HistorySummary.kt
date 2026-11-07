package io.github.kabos.pomodoro.history

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** 1 日分のまとめ。[records] は古い順。 */
data class DaySummary(val date: LocalDate, val records: List<PomodoroRecord>) {
  /** 最後まで終えたポモの数。途中で止めたものは数えない。 */
  val completedCount: Int get() = records.count { it.completed }
}

/**
 * 記録を日付・週でまとめる。Android に依存しないので単体テストで守る。
 * 日付は作業を始めた時刻で決める（23:50 に始めたポモはその日の分）。週は月曜始まり。
 */
object HistorySummary {

  /** 記録を日ごとにまとめ、新しい日から並べる。記録のない日は含めない。 */
  fun byDay(records: List<PomodoroRecord>, zone: ZoneId): List<DaySummary> =
    records
      .groupBy { it.startAt.toLocalDate(zone) }
      .map { (date, items) -> DaySummary(date, items.sortedBy { it.startAt }) }
      .sortedByDescending { it.date }

  fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

  /** [today] を含む週の月曜〜日曜それぞれの完了数。未来の日は 0。 */
  fun weekCounts(records: List<PomodoroRecord>, today: LocalDate, zone: ZoneId): List<Pair<LocalDate, Int>> {
    val counts = records.filter { it.completed }.groupingBy { it.startAt.toLocalDate(zone) }.eachCount()
    val monday = weekStart(today)
    return (0L until 7L).map { offset ->
      val date = monday.plusDays(offset)
      date to (counts[date] ?: 0)
    }
  }

  private fun Long.toLocalDate(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
}
