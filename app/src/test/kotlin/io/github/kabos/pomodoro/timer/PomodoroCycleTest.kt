package io.github.kabos.pomodoro.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PomodoroCycleTest {
  private val durations = Durations(workMs = 25 * 60_000L, meditationMs = 5 * 60_000L)
  private val t0 = 1_000_000_000_000L

  @Test
  fun `start begins a full work phase`() {
    val state = PomodoroCycle.start(t0, durations)
    assertEquals(Phase.WORK, state.phase)
    assertEquals(t0 + durations.workMs, state.phaseEndAt)
    assertEquals(0, state.completedWorkCount)
  }

  @Test
  fun `work end moves to meditation and counts the pomodoro`() {
    val work = PomodoroCycle.start(t0, durations)
    val meditation = PomodoroCycle.advance(work, work.phaseEndAt, durations)
    assertEquals(Phase.MEDITATION, meditation.phase)
    assertEquals(work.phaseEndAt + durations.meditationMs, meditation.phaseEndAt)
    assertEquals(1, meditation.completedWorkCount)
  }

  @Test
  fun `meditation end starts the next work immediately`() {
    val work = PomodoroCycle.start(t0, durations)
    val meditation = PomodoroCycle.advance(work, work.phaseEndAt, durations)
    val nextWork = PomodoroCycle.advance(meditation, meditation.phaseEndAt, durations)
    assertEquals(Phase.WORK, nextWork.phase)
    assertEquals(meditation.phaseEndAt + durations.workMs, nextWork.phaseEndAt)
    assertEquals(1, nextWork.completedWorkCount)
  }

  @Test
  fun `late alarm still gives the full next phase from now`() {
    val work = PomodoroCycle.start(t0, durations)
    val lateBy = 90_000L
    val meditation = PomodoroCycle.advance(work, work.phaseEndAt + lateBy, durations)
    assertEquals(work.phaseEndAt + lateBy + durations.meditationMs, meditation.phaseEndAt)
  }

  @Test
  fun `stop clears everything and restart does not carry remaining time`() {
    val work = PomodoroCycle.start(t0, durations)
    val stopped = PomodoroCycle.stop()
    assertFalse(stopped.isActive)
    assertEquals(0L, stopped.remainingMs(t0))

    val restarted = PomodoroCycle.start(t0 + 60_000L, durations, carriedCount = 4)
    assertEquals(t0 + 60_000L + durations.workMs, restarted.phaseEndAt)
    assertEquals(4, restarted.completedWorkCount)
    assertEquals(work.phaseEndAt + 60_000L, restarted.phaseEndAt)
  }

  @Test
  fun `advance on idle is a no-op`() {
    val idle = TimerState()
    assertEquals(idle, PomodoroCycle.advance(idle, t0, durations))
  }

  @Test
  fun `remaining never goes negative`() {
    val work = PomodoroCycle.start(t0, durations)
    assertEquals(durations.workMs, work.remainingMs(t0))
    assertEquals(0L, work.remainingMs(work.phaseEndAt + 1))
  }
}
