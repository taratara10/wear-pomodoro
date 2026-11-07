package io.github.kabos.pomodoro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import io.github.kabos.pomodoro.timer.Phase
import io.github.kabos.pomodoro.timer.TimerController
import io.github.kabos.pomodoro.timer.TimerState
import io.github.kabos.pomodoro.timer.label
import kotlinx.coroutines.delay

@Composable
fun PomodoroApp(controller: TimerController) {
  val state by controller.state.collectAsStateWithLifecycle()
  PomodoroScreen(
    state = state,
    todayCount = remember(state) { controller.todayCount() },
    onStart = controller::start,
    onStop = controller::stop,
  )
}

/**
 * 1 画面だけ。フェーズ・残り時間・何ポモ目かを表示し、下端のボタンで開始/停止する。
 * 残り時間は保存された終了時刻から毎秒計算する（画面が閉じていても進行は変わらない）。
 */
@Composable
fun PomodoroScreen(
  state: TimerState,
  todayCount: Int,
  onStart: () -> Unit,
  onStop: () -> Unit,
) {
  var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(state.phase, state.phaseEndAt) {
    while (state.isActive) {
      now = System.currentTimeMillis()
      delay(1_000)
    }
  }

  MaterialTheme {
    AppScaffold {
      // スクロールしない 1 画面なので、EdgeButton は Column の末尾に直接置く
      ScreenScaffold(timeText = { TimeText() }) { contentPadding ->
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Text(
              text = state.phase.label(),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
            Text(
              text = formatRemaining(state.remainingMs(now)),
              style = MaterialTheme.typography.numeralMedium,
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center,
            )
            Text(
              text = "今日 $todayCount ポモ",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
          EdgeButton(
            onClick = if (state.isActive) onStop else onStart,
            buttonSize = EdgeButtonSize.Medium,
          ) {
            Text(if (state.isActive) "停止" else "開始")
          }
        }
      }
    }
  }
}

private fun formatRemaining(ms: Long): String {
  val totalSeconds = (ms + 999) / 1_000
  return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@WearPreviewDevices
@Composable
private fun PomodoroScreenWorkPreview() {
  PomodoroScreen(
    state = TimerState(Phase.WORK, System.currentTimeMillis() + 17 * 60 * 1000L, 3),
    todayCount = 3,
    onStart = {},
    onStop = {},
  )
}

@WearPreviewDevices
@Composable
private fun PomodoroScreenIdlePreview() {
  PomodoroScreen(state = TimerState(), todayCount = 0, onStart = {}, onStop = {})
}
