# サードパーティ依存とライセンス方針

このアプリは**有料版を含めて商用配布する**。そのため依存ライブラリと素材は
「商用配布できるライセンス」に限定する。

## 許可するライセンス

`app/build.gradle.kts` の `licensee` ブロックが唯一の正。ここに書いたものだけが通る。

- Apache-2.0
- MIT
- BSD-2-Clause
- BSD-3-Clause
- CC0-1.0

`./gradlew :app:licensee` が CI で走り、許可リスト外のライセンス（またはライセンス表記の無い依存）が
混ざるとビルドが落ちる。「気づかずに GPL を混ぜる」事故を防ぐのが目的。

## 使ってはいけないもの

| ライブラリ | ライセンス | 代替 |
|---|---|---|
| FFTW | GPL（商用ライセンスは別売） | **kissfft**（BSD-3-Clause）または PFFFT |
| iText 7 | AGPL | `android.graphics.pdf.PdfDocument`（標準）／PdfBox-Android（Apache-2.0） |
| JTransforms | MPL/LGPL/GPL のトライライセンス | kissfft |
| 非商用限定の音源・アイコン・フォント | — | 端末標準フォント＋自作ベクター |

素材（アイコン、フォント、効果音、測定用音源）も同じ基準で扱い、採用したものはこのファイルに出典を残す。

## 現在の主な依存

| 依存 | 用途 | ライセンス |
|---|---|---|
| Android Gradle Plugin 9.3.1 | ビルド | Apache-2.0 |
| Kotlin 2.3.20 / Coroutines 1.11.0 | 言語・非同期 | Apache-2.0 |
| Jetpack Compose (BOM 2026.06.01) | UI | Apache-2.0 |
| AndroidX Navigation / Lifecycle / Activity / DataStore | 基盤 | Apache-2.0 |
| Room 2.8.4 | ローカルDB | Apache-2.0 |
| Dagger Hilt 2.60.1 | DI | Apache-2.0 |
| KSP 2.3.11 | アノテーション処理 | Apache-2.0 |
| kotlinx.serialization 1.11.0 | 型安全ナビゲーション | Apache-2.0 |
| protobuf-javalite（DataStore の推移依存） | 設定の永続化 | BSD-3-Clause |
| JUnit4 / Truth / Turbine | テスト（配布物に含まれない） | EPL-1.0 / Apache-2.0 / Apache-2.0 |
| app.cash.licensee 1.14.1 | ライセンス検査（ビルド時のみ） | Apache-2.0 |

## これから入れる予定（Phase ごと）

| Phase | 依存 | ライセンス | 備考 |
|---|---|---|---|
| 1 | Oboe（AAudio ラッパー） | Apache-2.0 | 低遅延オーディオI/O |
| 1 | kissfft | BSD-3-Clause | FFT。FFTW は使わない |
| 3 | PdfBox-Android（必要になれば） | Apache-2.0 | 標準 PdfDocument で足りるなら不要 |
| 5 | Google Play Billing 9 | Android SDK 利用規約 | `licensee` に `allowUrl` を追加する必要がある |
| 6 | ZXing（QR） | Apache-2.0 | 機材台帳のバーコード読み取り |
