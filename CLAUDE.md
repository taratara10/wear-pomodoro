# CLAUDE.md

Pixel Watch 向けのポモドーロアプリ（Wear OS、Kotlin、Wear Compose Material3）。
仕様と経緯は `docs/pomodoro_spec.md`（Obsidian vault への symlink）にある。フェーズ 5 以降の決定はそこに追記する。

## 構成

- `app/src/main/kotlin/io/github/kabos/pomodoro/timer/` — タイマーの中身
  - `PomodoroCycle.kt`: Android 非依存の状態遷移。ここを単体テストで守る
  - `TimerController.kt`: UI と Receiver が呼ぶ唯一の入口
  - `AlarmScheduler.kt` / `Haptics.kt` / `TimerStore.kt` / `EventLog.kt`
- `PomodoroApp.kt`: 1 画面の Compose UI
- `AlarmReceiver.kt` / `SystemEventReceiver.kt`: アラーム受信、再起動・更新後の復元
- `app/src/debug/`: 試験用に作業/瞑想の秒数を adb から変える Receiver

## コマンド

- 実機で起動: `android run`（`android` CLI）
- 単体テスト: `./gradlew :app:testDebugUnitTest`
- ビルド確認: `./gradlew :app:assembleDebug`

## 方針

- 残り時間はプロセス内で数えない。終了時刻を保存し、AlarmManager で起こす
- 「押さなくていい」を守る。操作が増える変更は仕様ノートで先に検討する
