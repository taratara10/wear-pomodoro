plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.ksp)
  alias(libs.plugins.room)
}

android {
  namespace = "io.github.kabos.pomodoro"
  compileSdk = 37
  defaultConfig {
    applicationId = "io.github.kabos.pomodoro"
    // Pixel Watch 2 / 3 (Wear OS 4 以降 = API 33+) を対象にする
    minSdk = 33
    targetSdk = 37
    versionCode = 1
    versionName = "0.1"
  }

  buildTypes {
    release {
      // 自分の時計に入れるだけなので debug の鍵で署名する。
      // 同じ鍵なら debug 版の上に入れ直せて、時計内の状態と記録が消えない
      signingConfig = signingConfigs.getByName("debug")
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = false
    shaders = false
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

// スキーマを残して、将来の DB 変更でマイグレーションを書けるようにする
room {
  schemaDirectory("$projectDir/schemas")
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)

  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.wear.compose.material3)
  implementation(libs.wear.compose.foundation)
  implementation(libs.wear.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.tooling)

  // 通知欄・文字盤に進行中のタイマーを出す
  implementation(libs.wear.ongoing)
  // 今日の状況を見るタイル
  implementation(libs.wear.tiles)
  implementation(libs.wear.protolayout)
  implementation(libs.wear.protolayout.expression)
  implementation(libs.androidx.concurrent.futures)
  // 文字盤のコンプリケーション（今日のポモ数・残り時間）
  implementation(libs.wear.complications.data.source)

  // 画面の行き来（タイマー → 記録 → 日ごとの詳細）
  implementation(libs.wear.compose.navigation)
  // ポモの開始・終了時刻の記録
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
