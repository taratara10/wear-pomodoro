package io.github.kabos.pomodoro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import io.github.kabos.pomodoro.history.PomodoroHistory
import io.github.kabos.pomodoro.timer.TimerController

class MainActivity : ComponentActivity() {
  private lateinit var controller: TimerController

  // 許可が出たら、動いているサイクルの通知をすぐ出す
  private val requestNotificationPermission =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) controller.ensureScheduled("notification_granted")
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    controller = TimerController(this)
    setContent {
      PomodoroApp(controller = controller, history = PomodoroHistory.get(this))
    }
    // 通知欄・文字盤から戻れるようにするため。初回の 1 回だけ聞かれる
    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
      requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  override fun onResume() {
    super.onResume()
    // 開発中の再インストールなどで予約が消えていても、画面を開けば自己修復する
    controller.ensureScheduled("resume")
  }
}
