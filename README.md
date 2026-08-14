# PA Toolbox

ライブ・イベント音響（PA）の現場で使う道具を Android 1台に集約するアプリ。
SPLメーター・RTA・シグナルジェネレータからパッチ表・進行表・見積までを1つに入れる。

- 有料版（Pro）あり：買い切り（ライフタイム）＋ 月額/年額サブスクの併売
- 業務・有償の現場での利用を許諾する（ただし計量法上の「取引・証明」用途は不可。後述）
- 依存ライブラリは商用配布可のライセンスのみ。CI で機械的に検査する

実装プランの全体像は `C:\Users\duffy\.claude\plans\fluffy-crafting-dewdrop.md` を参照。

---

## 現在の状態: Phase 0〜3 完了、Phase 4 ほぼ完了（35ツール中27が実装済み）

| Phase | 内容 | 状態 |
|---|---|---|
| 0 | マルチモジュール構成、テーマ3種、ツールランチャー、ProGate、CI | 実装済み |
| 1 | 計測コア（SPL / RTA / シグナルジェネレータ / チューナー / メトロノーム / 校正） | 実装済み |
| 2 | 計算機4種（ディレイ / BPM / dB換算 / インピーダンス） | 実装済み |
| 2 | リファレンス4種（結線図 / 帯域チャート / トラブルシュート / 用語辞典） | 実装済み |
| 3 | 案件管理 / パッチ表 / 進行表 / 本番タイマー / PDF出力 | 実装済み |
| 4 | SPLロガー / ハウリング検出 / 電源計算 / カバレッジ | 実装済み |
| 4 | ディレイ実測 / 極性チェック / 残響測定（IR・RT60） | 実装済み（実機未検証） |
| 4 | FFT アナライザ / スペクトログラム | 実装済み |
| 4 | ステージプロット | 未着手 |
| 4 | ワイヤレス周波数調整 | 保留（総務省の一次資料での確認が先） |
| 5 | 課金（Play Billing 9）・法務・Play 公開 | 未着手 |
| 6 | 録音、機材台帳、見積、稼働記録、クラウドバックアップ | 未着手 |

35ツールの一覧は [ToolId.kt](core/model/src/main/kotlin/com/patoolbox/core/model/ToolId.kt) が唯一の定義。
ホーム画面には未実装ツールも「準備中」として並ぶ（何がいつ来るか分かる方が実用的なので、非表示にしていない）。

---

## ビルド

### 必要なもの

| | バージョン | 備考 |
|---|---|---|
| JDK | 17 以上（推奨 21） | Android Studio 同梱の JBR でよい |
| Gradle | 9.7.0 | wrapper が入っているので個別インストールは不要 |
| Android SDK | Platform 36 / Build-Tools 36.0.0 | AGP が足りない分を取得する |

**Android Studio から**: プロジェクトを開いて Sync → Run。JDK は Studio の JBR が使われる。

**コマンドラインから**: PATH の Java が 8 などだと動かないので `JAVA_HOME` を明示する。

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew assembleDebug
```

### よく使うタスク

```bash
./gradlew assembleDebug
```

```bash
./gradlew testDebugUnitTest
```

```bash
./gradlew :app:licensee
```

`:app:licensee` は依存ライブラリのライセンスを検査する。許可リスト（[app/build.gradle.kts](app/build.gradle.kts) の `licensee` ブロック）に無いライセンスが混ざるとビルドが落ちる。

---

## モジュール構成

```
app                     ナビゲーション・DIルート・ツール振り分け
build-logic/convention  Gradle convention plugin（各モジュールの定型設定）
core/
  model                 純Kotlin。ToolId カタログ、ProStatus、校正プロファイル
  dsp                   純Kotlin。A/C重み、FFT、オクターブバンド、騒音計、信号生成、ピッチ検出
  calc                  純Kotlin。音速・ディレイ・BPM・dB換算・インピーダンス
  reference             純Kotlin。結線図・帯域チャート・切り分け・用語辞典の内容
  audio                 AudioRecord / AudioTrack。core:dsp を Android の音声APIに繋ぐ
                        スイープ測定（再生と録音を同時に回す一発測定）もここ
  designsystem          テーマ4種、寸法、BigReadout（巨大数値表示）
  ui                    ToolCard、チップ、権限ゲート、校正バッジ
  data                  設定（DataStore）、校正値（Room）、BuildInfo
  database              Room（案件 / 校正値 / パッチ表 / 進行表）
  export                PDF出力（Android標準の PdfDocument。iText は AGPL なので不採用）
  billing               ProGate（Phase 5 で Play Billing に差し替え）
  testing               Fake / MainDispatcherRule
feature/
  home                  ツールランチャー（検索・お気に入り・カテゴリ別）
  settings              テーマ切替、計測設定、Pro状態、免責表示
  spl                   SPLメーター（Leq / Lmax / 統計レベル / A-C-Z / F-S-I）
  calc                  計算機4種（タブ構成）
  reference             リファレンス4種（タブ構成）
  feedback              ハウリング検出（Pro）
  measure               ディレイ実測 / 極性チェック / 残響測定（Pro）
                        3ツールとも測定行為は「スイープ1回」なので1モジュールにまとめている
  analyzer              FFT アナライザ / スペクトログラム（Pro）
                        同じ解析結果の別の見せ方なので取り込みと解析を共有
  job                   案件管理（一覧・詳細）
  patch                 パッチ表（一覧・行編集）
  schedule              進行表（時刻は長さから自動計算）
  showtimer             本番タイマー
  rta                   RTA（1/1・1/3、Proで1/6・1/12、ピークホールド）
  siggen                シグナルジェネレータ（サイン/ノイズ/スイープ/バースト）
  tuner                 チューナー（音名・セント・A基準可変）
  metronome             メトロノーム（タップテンポ・拍子・アクセント）
  calibration           マイク校正（騒音計合わせ / 音響校正器）
```

**数値の正しさは `core:dsp` と `core:calc` の JVM テストで担保している**（全体で327テスト）。
Android を挟まないので、A特性の減衰量やピンクノイズのフラット性を
理論値と直接突き合わせられる。DSP を変更したらまずここを回すこと。

残響測定は「既知の残響時間を持つ部屋」を合成して検証している。
指数減衰する雑音をスイープに畳み込んで録音を作り、解析側がその残響時間を
復元できるかを見る（[RoomAnalysisTest](core/dsp/src/test/kotlin/com/patoolbox/core/dsp/RoomAnalysisTest.kt)）。
実機が無くても、暗騒音を足したときの挙動まで含めて詰められる。

```bash
./gradlew :core:dsp:test :core:calc:test
```

モジュールを追加するときは `settings.gradle.kts` に `include` して、
`build.gradle.kts` で convention plugin を1行適用するだけでよい。

```kotlin
plugins {
    alias(libs.plugins.pa.android.feature)
}

android {
    namespace = "com.patoolbox.feature.spl"
}
```

---

## 設計上の決めごと

- **電波が無くても全機能動く**: 課金判定・データ・帳票生成はローカル完結。Pro の購入情報はキャッシュし、猶予期間中はオフラインでも Pro を維持する（現場は圏外が多い）
- **現場で見える／押せる**: テーマは通常/ライト/ダークに加えて「暗所（赤）」「屋外（高コントラスト）」を用意。タップ領域は 48dp 以上
- **アイコンではなく文字バッジ**: ツール識別は `SPL` `RTA` `Ω` のような文字。暗所・屋外での識別性を優先した結果で、material-icons-extended（約30MB）を抱えずに済む
- **外部フォント・外部素材を使わない**: 商用配布時のライセンス確認を増やさないため、端末標準フォントと自作ベクターのみ

---

## 計測精度と利用範囲（重要）

スマートフォンの内蔵マイクには AGC・周波数特性の癖・最大SPLの制約があり、**無校正の測定値は数dB〜十数dBずれる**。
本アプリの測定値は現場調整・社内記録のための参考値であり、
**計量法上の「取引・証明」に用いる測定（騒音規制法の届出など）には使用できない**。
法定の測定には検定を受けた騒音計を使うこと。

一方で、**業務・有償の現場で参考測定として使うことは許諾する**。
この方針はアプリ内（設定画面）にも常時表示している。

Phase 1 では校正機能（手動オフセット / 校正器 / 端末別補正カーブ / 外部USBマイク）を実装し、
UI に校正状態を常時出す。

### ディレイ実測の絶対値は出していない

スイープを鳴らして録るまでの間に、端末の入出力バッファとアナログ段で
数十msの遅れが入る。量は端末ごとに違い、アプリからは完全には測れない。
そのため **1回の測定で出る数値を「ディレイタイム」として出すことはしていない**。

代わりに、基準点で1回測ってから移動して測り、**その差**を出す作りにした。
同じ端末の遅れは引き算で消えるので、校正なしで正しい値になる。
ディレイタワーの追い込みはもともとこの引き算なので、現場の手順とも一致する。

---

## 公開前にやること

- [ ] `applicationId` を自分のドメインに変更（`com.patoolbox` → 実際のもの。後から変更できない）
- [ ] targetSdk 36 の維持（2026/8/31 以降 Google Play で必須）
- [ ] Play Billing 8 以上（同日以降必須。現在の最新は 9）
- [ ] プライバシーポリシー、Data safety フォーム（マイク・録音の扱い）
- [ ] 特定商取引法に基づく表記（有料販売のため）
- [ ] EULA（商用利用の許諾 ＋ 計量法上の証明用途への不使用と免責）
- [ ] アプリ内 OSS ライセンス一覧（`:app:licensee` の出力を利用）
