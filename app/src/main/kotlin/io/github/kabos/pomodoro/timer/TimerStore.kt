package io.github.kabos.pomodoro.timer

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * [TimerState] と [Durations] を SharedPreferences に永続化し、UI には StateFlow で流す。
 * Activity と BroadcastReceiver は同じプロセスなので、プロセス単位のシングルトンにする。
 */
class TimerStore private constructor(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("timer", Context.MODE_PRIVATE)

  private val _state = MutableStateFlow(load())
  val state: StateFlow<TimerState> = _state

  fun update(state: TimerState) {
    prefs.edit()
      .putString(KEY_PHASE, state.phase.name)
      .putLong(KEY_PHASE_END_AT, state.phaseEndAt)
      .putInt(KEY_COUNT, state.completedWorkCount)
      .apply()
    _state.value = state
  }

  var durations: Durations
    get() = Durations(
      workMs = prefs.getLong(KEY_WORK_MS, Durations.DEFAULT.workMs),
      meditationMs = prefs.getLong(KEY_MEDITATION_MS, Durations.DEFAULT.meditationMs),
    )
    set(value) {
      prefs.edit()
        .putLong(KEY_WORK_MS, value.workMs)
        .putLong(KEY_MEDITATION_MS, value.meditationMs)
        .apply()
    }

  /** 何ポモ目かを日またぎでリセットするために、最後に数えた日を覚えておく。 */
  var countDay: Long
    get() = prefs.getLong(KEY_COUNT_DAY, 0L)
    set(value) { prefs.edit().putLong(KEY_COUNT_DAY, value).apply() }

  private fun load(): TimerState = TimerState(
    phase = prefs.getString(KEY_PHASE, null)?.let { runCatching { Phase.valueOf(it) }.getOrNull() }
      ?: Phase.IDLE,
    phaseEndAt = prefs.getLong(KEY_PHASE_END_AT, 0L),
    completedWorkCount = prefs.getInt(KEY_COUNT, 0),
  )

  companion object {
    private const val KEY_PHASE = "phase"
    private const val KEY_PHASE_END_AT = "phase_end_at"
    private const val KEY_COUNT = "completed_work_count"
    private const val KEY_COUNT_DAY = "count_day"
    private const val KEY_WORK_MS = "work_ms"
    private const val KEY_MEDITATION_MS = "meditation_ms"

    @Volatile private var instance: TimerStore? = null

    fun get(context: Context): TimerStore =
      instance ?: synchronized(this) {
        instance ?: TimerStore(context.applicationContext).also { instance = it }
      }
  }
}
