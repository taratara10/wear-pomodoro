package io.github.kabos.pomodoro.timer

import android.content.Context
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 切り替えの履歴を 1 行ずつファイルに残す。
 * 「1 日使って途切れなかったか」を本人の記憶ではなく記録で確かめるため。
 *
 * 取り出し方: adb shell run-as io.github.kabos.pomodoro cat files/events.log
 */
class EventLog(context: Context) {
  private val file = File(context.filesDir, "events.log")
  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

  fun append(event: String, state: TimerState? = null, now: Long = System.currentTimeMillis()) {
    val suffix = state?.let {
      " phase=${it.phase} endAt=${formatter.format(Instant.ofEpochMilli(it.phaseEndAt))} count=${it.completedWorkCount}"
    } ?: ""
    runCatching {
      trimIfLarge()
      file.appendText("${formatter.format(Instant.ofEpochMilli(now))} $event$suffix\n")
    }
  }

  fun readAll(): String = runCatching { file.readText() }.getOrDefault("")

  private fun trimIfLarge() {
    if (!file.exists() || file.length() < MAX_BYTES) return
    val lines = file.readLines()
    file.writeText(lines.drop(lines.size / 2).joinToString("\n", postfix = "\n"))
  }

  private companion object {
    const val MAX_BYTES = 256 * 1024L
  }
}
