package io.github.kabos.pomodoro.timer

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager

/**
 * 手首の振動だけで「作業が終わった」「瞑想が終わった」を区別できるようにする。
 * 画面を見ず、操作もしないで済ませるのが狙い。
 *
 * - 作業終了: 長い振動 3 回（ゆっくり。手を止めて瞑想に入る合図）
 * - 瞑想終了: 短い振動 5 回（軽く。次の作業はもう始まっている）
 *
 * USAGE_ALARM にすると、通知の制限やサイレントモードの影響を受けにくい。
 */
class Haptics(context: Context) {
  private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
  private val attributes = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)

  fun workEnded() = vibrate(longArrayOf(0, 700, 300, 700, 300, 700))

  fun meditationEnded() = vibrate(longArrayOf(0, 120, 120, 120, 120, 120, 120, 120, 120, 120))

  private fun vibrate(pattern: LongArray) {
    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1), attributes)
  }
}
