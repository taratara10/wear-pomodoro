package io.github.kabos.pomodoro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.kabos.pomodoro.timer.TimerController

class MainActivity : ComponentActivity() {
  private lateinit var controller: TimerController

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    controller = TimerController(this)
    setContent {
      PomodoroApp(controller = controller)
    }
  }

  override fun onResume() {
    super.onResume()
    // 開発中の再インストールなどで予約が消えていても、画面を開けば自己修復する
    controller.ensureScheduled("resume")
  }
}
