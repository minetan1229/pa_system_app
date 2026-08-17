plugins {
    alias(libs.plugins.pa.jvm.library)
}

dependencies {
    // 解説（HelpTopic）を ToolId で引けるようにするため。
    // core:model は Android に依存しない純 Kotlin なので、この層の性質は変わらない
    api(project(":core:model"))
}

// 結線図・帯域チャート・トラブルシュート・用語辞典・各機能の解説の内容。
//
// 文言を strings.xml ではなく Kotlin のデータで持っている。
// UI の文言（ボタン名など）はリソースに置くが、こちらは「アプリの中身そのもの」で
// 量が多く構造を持つため、データとして書いたほうが編集も検証もしやすい。
// 英語対応するときは表示層で辞書を差し替える。
