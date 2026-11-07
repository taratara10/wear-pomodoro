# AGENTS.md

Pixel Watch 向けのポモドーロアプリ（Wear OS、Kotlin、Wear Compose Material3）。
仕様と経緯は `docs/pomodoro_spec.md`（Obsidian vault への symlink）にある。フェーズ 5 以降の決定はそこに追記する。

## 構成

- `app/src/main/kotlin/io/github/kabos/pomodoro/timer/` — タイマーの中身
  - `PomodoroCycle.kt`: Android 非依存の状態遷移。ここを単体テストで守る
  - `TimerController.kt`: UI と Receiver が呼ぶ唯一の入口
  - `AlarmScheduler.kt` / `Haptics.kt` / `TimerStore.kt` / `EventLog.kt`
  - `TimerNotifier.kt`: 常駐通知と Ongoing Activity（文字盤の表示）
- `app/src/main/kotlin/io/github/kabos/pomodoro/history/` — ポモの記録（Room）
  - `HistorySummary.kt`: Android 非依存の日・週の集計。単体テストで守る
  - `PomodoroHistory.kt`: 読み書きの入口。Receiver から書くときは `afterPendingWrites` で goAsync を閉じる
  - DB を変えたら `version` を上げてマイグレーションを書く。スキーマは `app/schemas/` に出る
- `PomodoroApp.kt`: 画面遷移とタイマー画面。`HistoryScreens.kt`: 記録と日ごとの詳細
- `PomodoroTileService.kt`: 今日の状況を見るタイル。状態が変わると `TimerController` が更新を頼む
- `AlarmReceiver.kt` / `SystemEventReceiver.kt`: アラーム受信、再起動・更新後の復元
- `app/src/debug/`: 試験用に作業/瞑想の秒数を adb から変える Receiver

## コマンド

- 実機で起動: `android run`（`android` CLI）
- 単体テスト: `./gradlew :app:testDebugUnitTest`
- ビルド確認: `./gradlew :app:assembleDebug`

## 方針

- 残り時間はプロセス内で数えない。終了時刻を保存し、AlarmManager で起こす
- 「押さなくていい」を守る。操作が増える変更は仕様ノートで先に検討する

## 作業の進め方

- コミットは確認を取らずに、意味のまとまりごとに行ってよい。push はしない
- main（master）では作業せず、ブランチを切ってからコミットする
