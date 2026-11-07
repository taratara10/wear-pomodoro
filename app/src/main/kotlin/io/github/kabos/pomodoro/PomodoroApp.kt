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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import io.github.kabos.pomodoro.history.PomodoroHistory
import java.time.LocalDate
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

/**
 * 画面は 3 つ。タイマー → 記録（今週と日の一覧）→ 1 日の詳細。右スワイプで戻る。
 */
@Composable
fun PomodoroApp(controller: TimerController, history: PomodoroHistory) {
  val navController = rememberSwipeDismissableNavController()
  MaterialTheme {
    AppScaffold {
      SwipeDismissableNavHost(navController = navController, startDestination = ROUTE_TIMER) {
        composable(ROUTE_TIMER) {
          val state by controller.state.collectAsStateWithLifecycle()
          PomodoroScreen(
            state = state,
            todayCount = remember(state) { controller.todayCount() },
            onStart = controller::start,
            onStop = controller::stop,
            onOpenHistory = { navController.navigate(ROUTE_HISTORY) },
          )
        }
        composable(ROUTE_HISTORY) {
          HistoryRoute(history = history, onOpenDay = { date -> navController.navigate("$ROUTE_DAY/${date.toEpochDay()}") })
        }
        composable(
          "$ROUTE_DAY/{epochDay}",
          arguments = listOf(navArgument("epochDay") { type = NavType.LongType }),
        ) { entry ->
          val date = LocalDate.ofEpochDay(entry.arguments?.getLong("epochDay") ?: LocalDate.now().toEpochDay())
          DayRoute(history = history, date = date)
        }
      }
    }
  }
}

private const val ROUTE_TIMER = "timer"
private const val ROUTE_HISTORY = "history"
private const val ROUTE_DAY = "day"

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
  onOpenHistory: () -> Unit,
) {
  var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(state.phase, state.phaseEndAt) {
    while (state.isActive) {
      now = System.currentTimeMillis()
      delay(1_000)
    }
  }

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
        CompactButton(
          onClick = onOpenHistory,
          label = { Text("今日 $todayCount ポモ ›") },
          colors = ButtonDefaults.filledTonalButtonColors(),
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

private fun formatRemaining(ms: Long): String {
  val totalSeconds = (ms + 999) / 1_000
  return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@WearPreviewDevices
@Composable
private fun PomodoroScreenWorkPreview() {
  MaterialTheme {
    PomodoroScreen(
      state = TimerState(Phase.WORK, System.currentTimeMillis() + 17 * 60 * 1000L, 3),
      todayCount = 3,
      onStart = {},
      onStop = {},
      onOpenHistory = {},
    )
  }
}

@WearPreviewDevices
@Composable
private fun PomodoroScreenIdlePreview() {
  MaterialTheme {
    PomodoroScreen(state = TimerState(), todayCount = 0, onStart = {}, onStop = {}, onOpenHistory = {})
  }
}
