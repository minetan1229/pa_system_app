plugins {
    alias(libs.plugins.pa.jvm.library)
}

// 計算機の中身。core:dsp と同じ方針で Android に依存させない。
// 「音速の温度補正」「dBu↔V」「並列インピーダンス」のような数値は
// 間違っていても画面上は正しそうに見えるので、JVMテストで固めるのが前提。
