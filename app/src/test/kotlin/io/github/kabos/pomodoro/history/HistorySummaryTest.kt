package io.github.kabos.pomodoro.history

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class HistorySummaryTest {
  private val zone = ZoneId.of("Asia/Tokyo")

  private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) =
    LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

  private fun record(start: Long, completed: Boolean = true) =
    PomodoroRecord(startAt = start, endAt = start + 25 * 60_000L, completed = completed)

  @Test
  fun `records are grouped by the local day they started, newest day first`() {
    val records = listOf(
      record(at(2026, 11, 6, 23, 50)), // 0 時をまたいでも始めた日の分
      record(at(2026, 11, 7, 13, 30)),
      record(at(2026, 11, 7, 9, 0)),
    )
    val days = HistorySummary.byDay(records, zone)
    assertEquals(listOf(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 6)), days.map { it.date })
    assertEquals(listOf(at(2026, 11, 7, 9, 0), at(2026, 11, 7, 13, 30)), days[0].records.map { it.startAt })
  }

  @Test
  fun `stopped work is listed but not counted`() {
    val day = HistorySummary.byDay(
      listOf(record(at(2026, 11, 7, 9, 0)), record(at(2026, 11, 7, 10, 0), completed = false)),
      zone,
    ).single()
    assertEquals(2, day.records.size)
    assertEquals(1, day.completedCount)
  }

  @Test
  fun `week runs monday to sunday and counts only completed`() {
    val saturday = LocalDate.of(2026, 11, 7)
    val records = listOf(
      record(at(2026, 11, 1, 10, 0)), // 前の週の日曜
      record(at(2026, 11, 2, 10, 0)), // 月曜
      record(at(2026, 11, 7, 10, 0)),
      record(at(2026, 11, 7, 11, 0)),
      record(at(2026, 11, 7, 12, 0), completed = false),
    )
    val week = HistorySummary.weekCounts(records, saturday, zone)
    assertEquals(LocalDate.of(2026, 11, 2), week.first().first)
    assertEquals(LocalDate.of(2026, 11, 8), week.last().first)
    assertEquals(listOf(1, 0, 0, 0, 0, 2, 0), week.map { it.second })
  }
}
