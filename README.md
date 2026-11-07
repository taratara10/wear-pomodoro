# Pomodoro (Wear OS)

Pixel Watch 単体で動くポモドーロ。25分作業 / 5分瞑想を自動で繰り返し、手首の振動だけで切り替えを伝える。
背景と仕様は Obsidian の `050_develop/pomodoro/pomodoro_spec.md`。

## 動き

- 開始ボタンは最初の 1 回だけ。以降は 25分 → 5分 → 25分 … を自動で回す
- 作業終了: 長い振動 3 回。瞑想終了: 短い振動 5 回
- 停止ボタンでサイクルを止める。再開は新しい 25分から
- 切り替えは AlarmManager の `setAlarmClock` で予約し、状態は SharedPreferences に保存する。プロセスが死んでも続く
- 切り替えの履歴は `files/events.log` に残る

## ビルドと実機実行

```sh
# Pixel Watch を Wi-Fi デバッグで接続（時計の開発者向けオプションでペアリング）
adb pair <ip>:<pair-port>
adb connect <ip>:<port>

# ビルドして時計で起動
android run

# 単体テスト
./gradlew :app:testDebugUnitTest
```

## 試験用コマンド（debug ビルドのみ）

```sh
# 作業 60秒 / 瞑想 20秒 に短縮（次の開始から有効）
adb shell am broadcast -a io.github.kabos.pomodoro.DEBUG_SET_DURATIONS \
  -n io.github.kabos.pomodoro/.DebugReceiver --ei work_sec 60 --ei meditation_sec 20

# 25分 / 5分 に戻す
adb shell am broadcast -a io.github.kabos.pomodoro.DEBUG_SET_DURATIONS \
  -n io.github.kabos.pomodoro/.DebugReceiver

# 切り替えの記録を見る
adb shell run-as io.github.kabos.pomodoro cat files/events.log
```
