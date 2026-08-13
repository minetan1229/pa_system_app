plugins {
    alias(libs.plugins.pa.android.library)
    alias(libs.plugins.pa.android.hilt)
}

android {
    namespace = "com.patoolbox.core.export"
}

dependencies {
    api(project(":core:model"))
}

// PDF は Android 標準の android.graphics.pdf.PdfDocument で書く。
// iText は AGPL なので有料アプリには使えない（THIRD_PARTY.md 参照）。
